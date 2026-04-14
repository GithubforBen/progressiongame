package com.financegame.service;

import com.financegame.dto.CharacterDto;
import com.financegame.dto.TurnResultDto;
import com.financegame.entity.*;
import com.financegame.repository.*;
import com.financegame.repository.ActiveEventRepository;
import com.financegame.repository.CollectibleRepository;
import com.financegame.repository.PlayerTravelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class TurnService {

    private final CharacterService characterService;
    private final CharacterRepository characterRepository;
    private final PlayerJobRepository playerJobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final MonthlyExpenseRepository monthlyExpenseRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final MonthlySnapshotRepository monthlySnapshotRepository;
    private final EventLogRepository eventLogRepository;
    private final StockService stockService;
    private final PlayerTravelRepository playerTravelRepository;
    private final ActiveEventRepository activeEventRepository;
    private final CollectibleRepository collectibleRepository;
    private final RandomEventService randomEventService;
    private final RelationshipService relationshipService;

    private final Random random = new Random();

    public TurnService(
        CharacterService characterService,
        CharacterRepository characterRepository,
        PlayerJobRepository playerJobRepository,
        JobApplicationRepository jobApplicationRepository,
        MonthlyExpenseRepository monthlyExpenseRepository,
        EducationProgressRepository educationProgressRepository,
        MonthlySnapshotRepository monthlySnapshotRepository,
        EventLogRepository eventLogRepository,
        StockService stockService,
        PlayerTravelRepository playerTravelRepository,
        ActiveEventRepository activeEventRepository,
        CollectibleRepository collectibleRepository,
        RandomEventService randomEventService,
        RelationshipService relationshipService
    ) {
        this.characterService = characterService;
        this.characterRepository = characterRepository;
        this.playerJobRepository = playerJobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.monthlyExpenseRepository = monthlyExpenseRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.monthlySnapshotRepository = monthlySnapshotRepository;
        this.eventLogRepository = eventLogRepository;
        this.stockService = stockService;
        this.playerTravelRepository = playerTravelRepository;
        this.activeEventRepository = activeEventRepository;
        this.collectibleRepository = collectibleRepository;
        this.randomEventService = randomEventService;
        this.relationshipService = relationshipService;
    }

    @Transactional
    public TurnResultDto endTurn(Long playerId) {
        GameCharacter character = characterService.findOrThrow(playerId);
        int currentTurn = character.getCurrentTurn();

        List<String> events = new ArrayList<>();
        List<TurnResultDto.LineItem> incomeBreakdown = new ArrayList<>();
        List<TurnResultDto.LineItem> expenseBreakdown = new ArrayList<>();

        // --- 1. Resolve job applications from last month ---
        resolveApplications(playerId, currentTurn, events);

        // --- 2. Calculate salary income ---
        BigDecimal grossIncome = calculateSalaries(playerId, currentTurn, incomeBreakdown, events);

        // --- 3. Progressive tax on income ---
        BigDecimal taxPaid = calculateTax(grossIncome);
        if (taxPaid.compareTo(BigDecimal.ZERO) > 0) {
            expenseBreakdown.add(new TurnResultDto.LineItem("Einkommensteuer", taxPaid));
        }

        // --- 4. Monthly expenses ---
        BigDecimal totalExpenses = deductExpenses(playerId, expenseBreakdown);
        totalExpenses = totalExpenses.add(taxPaid);

        // --- 4b. Health insurance risk ---
        BigDecimal medicalCost = applyHealthInsuranceRisk(playerId, events);
        if (medicalCost.compareTo(BigDecimal.ZERO) > 0) {
            expenseBreakdown.add(new TurnResultDto.LineItem("Arztrechnung (unversichert)", medicalCost));
            totalExpenses = totalExpenses.add(medicalCost);
        }

        // --- 5. Apply net cash change ---
        BigDecimal netChange = grossIncome.subtract(totalExpenses);
        character.setCash(character.getCash().add(netChange).max(BigDecimal.ZERO));

        // --- 5b. Random events (Step 11) ---
        randomEventService.applyRandomEvents(playerId, character, events);

        // --- 6. Needs decay (hunger/energy/happiness) ---
        applyNeedsDecay(character, playerId);

        // --- 6b. Relationship happiness bonus (Step 14) ---
        int happinessBonus = relationshipService.advanceRelationships(playerId, events);
        character.setHappiness(clamp(character.getHappiness() + happinessBonus));

        // --- 7. Stress from active jobs ---
        updateStress(character, playerId);

        // --- 8. Increment months worked & education ---
        advanceEducation(playerId, currentTurn, events);

        // --- 8c. Check travel arrival ---
        checkTravelArrival(playerId, currentTurn, events);

        // --- 8d. Expire old events, generate new ones ---
        activeEventRepository.deleteExpiredForPlayer(playerId, currentTurn);
        generateRandomEvent(playerId, currentTurn, events);

        // --- 9. Advance turn counter & persist character ---
        character.setCurrentTurn(currentTurn + 1);
        characterRepository.save(character);

        // --- 9b. Simulate stock prices (updates investments in DB) ---
        stockService.simulatePrices(currentTurn);

        // --- 9c. Recalculate net worth (cash + investments) and reload ---
        characterService.recalculateNetWorth(playerId);
        character = characterService.findOrThrow(playerId);

        // --- 10. Save monthly snapshot ---
        monthlySnapshotRepository.save(new MonthlySnapshot(
            playerId, currentTurn, character.getCash(), character.getNetWorth(),
            grossIncome, totalExpenses
        ));

        // --- 11. Log events ---
        for (String event : events) {
            eventLogRepository.save(new EventLog(playerId, event, null, "TURN", currentTurn));
        }

        return new TurnResultDto(
            CharacterDto.from(character),
            character.getCurrentTurn(),
            grossIncome,
            taxPaid,
            totalExpenses,
            netChange,
            incomeBreakdown,
            expenseBreakdown,
            events
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void resolveApplications(Long playerId, int currentTurn, List<String> events) {
        List<JobApplication> pending = jobApplicationRepository.findPendingByPlayerId(playerId);
        for (JobApplication app : pending) {
            if (app.getAppliedAtTurn() >= currentTurn) continue; // not yet a full month

            boolean accepted = evaluateApplication(playerId, app);
            app.setStatus(accepted ? "ACCEPTED" : "REJECTED");
            app.setResolvedAtTurn(currentTurn);
            jobApplicationRepository.save(app);

            if (accepted) {
                // Create active player_job entry
                PlayerJob pj = new PlayerJob(playerId, app.getJob(), currentTurn);
                playerJobRepository.save(pj);
                events.add("Bewerbung angenommen: " + app.getJob().getName());
            } else {
                events.add("Bewerbung abgelehnt: " + app.getJob().getName());
            }
        }
    }

    private boolean evaluateApplication(Long playerId, JobApplication app) {
        Job job = app.getJob();

        // Check education requirement
        boolean meetsEducation = meetsEducationRequirement(playerId,
            job.getRequiredEducationType(), job.getRequiredEducationField());

        // Check experience requirement
        boolean meetsExperience = true;
        if (job.getRequiredMonthsExperience() > 0) {
            int totalMonths = playerJobRepository.findAllByPlayerId(playerId)
                .stream()
                .mapToInt(PlayerJob::getMonthsWorked)
                .sum();
            meetsExperience = totalMonths >= job.getRequiredMonthsExperience();
        }

        // Acceptance probability based on qualification
        double probability;
        if (meetsEducation && meetsExperience) {
            probability = 0.80;
        } else if (meetsEducation || meetsExperience) {
            probability = 0.30;
        } else {
            probability = 0.05;
        }

        return random.nextDouble() < probability;
    }

    private boolean meetsEducationRequirement(Long playerId, String requiredType, String requiredField) {
        if (requiredType == null) return true;
        EducationProgress ep = educationProgressRepository.findByPlayerId(playerId).orElse(null);
        if (ep == null) return false;
        List<String> completed = Arrays.asList(ep.getCompletedStages());
        // Stage key format: "REALSCHULABSCHLUSS", "AUSBILDUNG_FACHINFORMATIKER", "BACHELOR_INFORMATIK", etc.
        String key = requiredField != null ? requiredType + "_" + requiredField : requiredType;
        return completed.contains(key);
    }

    private BigDecimal calculateSalaries(Long playerId, int currentTurn,
                                          List<TurnResultDto.LineItem> breakdown, List<String> events) {
        BigDecimal total = BigDecimal.ZERO;
        List<PlayerJob> activeJobs = playerJobRepository.findActiveByPlayerId(playerId);

        for (PlayerJob pj : activeJobs) {
            BigDecimal salary = pj.getJob().getSalary();
            total = total.add(salary);
            breakdown.add(new TurnResultDto.LineItem(pj.getJob().getName(), salary));
            // Increment experience
            pj.setMonthsWorked(pj.getMonthsWorked() + 1);
            playerJobRepository.save(pj);
        }

        if (activeJobs.isEmpty()) {
            events.add("Kein Job — kein Gehalt diesen Monat.");
        }

        return total;
    }

    /**
     * Progressive monthly income tax:
     *   0 – 1 000 €:   0 %
     *   1 001 – 3 000 €: 20 %
     *   3 001 – 6 000 €: 32 %
     *   > 6 000 €:       42 %
     */
    private BigDecimal calculateTax(BigDecimal income) {
        if (income.compareTo(BigDecimal.valueOf(1000)) <= 0) return BigDecimal.ZERO;

        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal[] brackets = {
            BigDecimal.valueOf(1000), BigDecimal.valueOf(2000), BigDecimal.valueOf(3000)
        };
        double[] rates = {0.20, 0.32, 0.42};

        BigDecimal remaining = income.subtract(BigDecimal.valueOf(1000));

        BigDecimal prev = BigDecimal.ZERO;
        for (int i = 0; i < brackets.length; i++) {
            BigDecimal top = brackets[i];
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal taxable = remaining.min(top.subtract(prev));
            tax = tax.add(taxable.multiply(BigDecimal.valueOf(rates[i])));
            remaining = remaining.subtract(taxable);
            prev = top;
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            tax = tax.add(remaining.multiply(BigDecimal.valueOf(0.42)));
        }

        return tax.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal deductExpenses(Long playerId, List<TurnResultDto.LineItem> breakdown) {
        BigDecimal total = BigDecimal.ZERO;
        for (MonthlyExpense expense : monthlyExpenseRepository.findActiveByPlayerId(playerId)) {
            total = total.add(expense.getAmount());
            breakdown.add(new TurnResultDto.LineItem(expense.getLabel(), expense.getAmount()));
        }
        return total;
    }

    private void applyNeedsDecay(GameCharacter character, Long playerId) {
        boolean hasFoodExpense = monthlyExpenseRepository.findActiveByPlayerId(playerId)
            .stream().anyMatch(e -> "ESSEN".equals(e.getCategory()));

        character.setHunger(clamp(character.getHunger() - (hasFoodExpense ? 5 : 15)));
        character.setEnergy(clamp(character.getEnergy() - 8));
        character.setHappiness(clamp(character.getHappiness() - 5));
    }

    private void updateStress(GameCharacter character, Long playerId) {
        List<PlayerJob> activeJobs = playerJobRepository.findActiveByPlayerId(playerId);
        if (activeJobs.isEmpty()) {
            // Natural stress recovery when unemployed
            character.setStress(clamp(character.getStress() - 10));
        } else {
            int totalStress = activeJobs.stream().mapToInt(pj -> pj.getJob().getStressPerMonth()).sum();
            character.setStress(clamp(totalStress));
        }
    }

    private void advanceEducation(Long playerId, int currentTurn, List<String> events) {
        educationProgressRepository.findByPlayerId(playerId).ifPresent(ep -> {
            // Main education
            if (ep.getMainStageMonthsRemaining() > 0) {
                ep.setMainStageMonthsRemaining(ep.getMainStageMonthsRemaining() - 1);
                if (ep.getMainStageMonthsRemaining() == 0) {
                    String stageKey = ep.getMainStageField() != null
                        ? ep.getMainStage() + "_" + ep.getMainStageField()
                        : ep.getMainStage();
                    addToCompletedStages(ep, stageKey);
                    events.add("Ausbildung abgeschlossen: " + ep.getMainStage());
                }
            }
            // Side certification
            if (ep.getSideCertMonthsRemaining() > 0) {
                ep.setSideCertMonthsRemaining(ep.getSideCertMonthsRemaining() - 1);
                if (ep.getSideCertMonthsRemaining() == 0) {
                    String certKey = ep.getSideCert();
                    addToCompletedStages(ep, certKey);
                    events.add("Weiterbildung abgeschlossen: " + ep.getSideCert());
                    ep.setSideCert(null);
                }
            }
            educationProgressRepository.save(ep);
        });
    }

    private void addToCompletedStages(EducationProgress ep, String stage) {
        String[] current = ep.getCompletedStages();
        String[] updated = Arrays.copyOf(current, current.length + 1);
        updated[current.length] = stage;
        ep.setCompletedStages(updated);
    }

    private void checkTravelArrival(Long playerId, int currentTurn, List<String> events) {
        playerTravelRepository.findByPlayerId(playerId).ifPresent(travel -> {
            if (travel.getArriveAtTurn() != null && travel.getArriveAtTurn() <= currentTurn + 1) {
                String dest = travel.getDestinationCountry();
                // Add to visited
                String[] visited = travel.getVisitedCountries();
                boolean alreadyVisited = Arrays.asList(visited).contains(dest);
                if (!alreadyVisited) {
                    String[] updated = Arrays.copyOf(visited, visited.length + 1);
                    updated[visited.length] = dest;
                    travel.setVisitedCountries(updated);
                }
                travel.setCurrentCountry(dest);
                travel.setDestinationCountry(null);
                travel.setArriveAtTurn(null);
                playerTravelRepository.save(travel);
                events.add("Angekommen in " + dest + "! Erkunde lokale Sammlerstücke.");
            }
        });
    }

    private void generateRandomEvent(Long playerId, int currentTurn, List<String> events) {
        if (random.nextDouble() > 0.20) return;
        List<Collectible> all = collectibleRepository.findAll();
        if (all.isEmpty()) return;
        Collectible c = all.get(random.nextInt(all.size()));
        // Check not already active
        boolean alreadyActive = activeEventRepository.findActiveForPlayer(playerId, currentTurn)
            .stream().anyMatch(e -> c.getId().equals(e.getCollectibleId()));
        if (alreadyActive) return;

        ActiveEvent event = new ActiveEvent();
        event.setPlayerId(playerId);
        event.setType("COLLECTIBLE_SALE");
        event.setCountry(c.getCountryRequired());
        event.setCollectibleId(c.getId());
        event.setExpiresAtTurn(currentTurn + 2);
        activeEventRepository.save(event);
        events.add("Tages-Event: \"" + c.getName() + "\" für 30% Rabatt — " + c.getCountryRequired()
            + " (endet Monat " + (currentTurn + 2) + ")!");
    }

    /**
     * 10% chance of a random medical bill if no active KRANKENVERSICHERUNG expense.
     * Bill is between 200 and 2000 €.
     */
    private BigDecimal applyHealthInsuranceRisk(Long playerId, List<String> events) {
        boolean hasKv = monthlyExpenseRepository.findActiveByPlayerId(playerId)
            .stream()
            .anyMatch(e -> "KRANKENVERSICHERUNG".equals(e.getCategory()));

        if (!hasKv && random.nextDouble() < 0.10) {
            int cost = 200 + random.nextInt(1801); // 200–2000
            events.add("Arztrechnung! Ohne Krankenversicherung zahlst du " + cost + " € aus eigener Tasche.");
            return BigDecimal.valueOf(cost);
        }
        return BigDecimal.ZERO;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
