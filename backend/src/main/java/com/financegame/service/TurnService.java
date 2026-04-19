package com.financegame.service;

import com.financegame.config.GameConfig;
import com.financegame.domain.effect.collection.CollectionBonusApplier;
import com.financegame.domain.events.*;
import com.financegame.dto.CharacterDto;
import com.financegame.dto.TurnResultDto;
import com.financegame.entity.*;
import com.financegame.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TurnService {

    private final CharacterService characterService;
    private final CharacterRepository characterRepository;
    private final PlayerJobRepository playerJobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final MonthlyExpenseRepository monthlyExpenseRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final MonthlySnapshotRepository monthlySnapshotRepository;
    private final StockService stockService;
    private final PlayerTravelRepository playerTravelRepository;
    private final ActiveEventRepository activeEventRepository;
    private final CollectibleRepository collectibleRepository;
    private final RandomEventService randomEventService;
    private final RelationshipService relationshipService;
    private final PlayerRealEstateRepository playerRealEstateRepository;
    private final PlayerLoanRepository playerLoanRepository;
    private final CollectionService collectionService;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, CollectionBonusApplier> bonusApplierMap;
    private final GameConfig gameConfig;
    private final TaxService taxService;

    private final Random random = new Random();

    public TurnService(
        CharacterService characterService,
        CharacterRepository characterRepository,
        PlayerJobRepository playerJobRepository,
        JobApplicationRepository jobApplicationRepository,
        MonthlyExpenseRepository monthlyExpenseRepository,
        EducationProgressRepository educationProgressRepository,
        MonthlySnapshotRepository monthlySnapshotRepository,
        StockService stockService,
        PlayerTravelRepository playerTravelRepository,
        ActiveEventRepository activeEventRepository,
        CollectibleRepository collectibleRepository,
        RandomEventService randomEventService,
        RelationshipService relationshipService,
        PlayerRealEstateRepository playerRealEstateRepository,
        PlayerLoanRepository playerLoanRepository,
        CollectionService collectionService,
        ApplicationEventPublisher eventPublisher,
        List<CollectionBonusApplier> bonusAppliers,
        GameConfig gameConfig,
        TaxService taxService
    ) {
        this.characterService = characterService;
        this.characterRepository = characterRepository;
        this.playerJobRepository = playerJobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.monthlyExpenseRepository = monthlyExpenseRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.monthlySnapshotRepository = monthlySnapshotRepository;
        this.stockService = stockService;
        this.playerTravelRepository = playerTravelRepository;
        this.activeEventRepository = activeEventRepository;
        this.collectibleRepository = collectibleRepository;
        this.randomEventService = randomEventService;
        this.relationshipService = relationshipService;
        this.playerRealEstateRepository = playerRealEstateRepository;
        this.playerLoanRepository = playerLoanRepository;
        this.collectionService = collectionService;
        this.eventPublisher = eventPublisher;
        this.bonusApplierMap = bonusAppliers.stream()
            .collect(Collectors.toMap(CollectionBonusApplier::getBonusType, Function.identity()));
        this.gameConfig = gameConfig;
        this.taxService = taxService;
    }

    @Transactional
    public TurnResultDto endTurn(Long playerId) {
        GameCharacter character = characterService.findOrThrow(playerId);
        int currentTurn = character.getCurrentTurn();

        List<String> events = new ArrayList<>();
        List<TurnResultDto.LineItem> incomeBreakdown = new ArrayList<>();
        List<TurnResultDto.LineItem> expenseBreakdown = new ArrayList<>();

        // --- 0. Jail / Exile ticks ---
        boolean inJail = character.getJailMonthsRemaining() > 0;
        if (inJail) {
            character.setJailMonthsRemaining(character.getJailMonthsRemaining() - 1);
            character.setStress(Math.min(100, character.getStress() + 15));
            character.setHappiness(Math.max(0, character.getHappiness() - 10));
            events.add("🔒 Du sitzt in Haft. " + character.getJailMonthsRemaining() + " Monate verbleiben.");
            if (character.getJailMonthsRemaining() == 0) {
                events.add("🔓 Du wurdest aus der Haft entlassen!");
            }
        }
        if (character.getExileMonthsRemaining() > 0) {
            character.setExileMonthsRemaining(character.getExileMonthsRemaining() - 1);
            events.add("✈️ Du lebst im Exil. " + character.getExileMonthsRemaining() + " Monate Mindestaufenthalt verbleiben.");
            if (character.getExileMonthsRemaining() == 0) {
                events.add("🏠 Dein Exil ist beendet. Du kannst zurückkehren.");
            }
        }

        // --- 1. Resolve job applications from last month ---
        resolveApplications(playerId, currentTurn, events);

        // --- 2. Calculate salary income ---
        BigDecimal grossIncome = inJail ? BigDecimal.ZERO
            : calculateSalaries(playerId, currentTurn, incomeBreakdown, events);
        if (inJail) events.add("Kein Gehalt — du befindest dich in Haft.");

        // --- 2a. Collection bonuses: income phase (SALARY_MULTIPLIER, MONTHLY_INCOME_BONUS) ---
        List<CollectionService.ActiveBonus> collectionBonuses =
            collectionService.getActiveCollectionBonuses(playerId);
        for (CollectionService.ActiveBonus bonus : collectionBonuses) {
            CollectionBonusApplier applier = bonusApplierMap.get(bonus.bonusType());
            if (applier != null) {
                grossIncome = applier.modifyIncome(grossIncome, bonus.bonusValue(), incomeBreakdown);
            }
        }

        // --- 2b. Rental income from RENTED_OUT properties ---
        BigDecimal rentalIncome = calculateRentalIncome(playerId, incomeBreakdown);
        grossIncome = grossIncome.add(rentalIncome);

        // --- 3. Progressive tax on income ---
        BigDecimal taxPaid = taxService.calculateTax(grossIncome);

        // --- 3b. Tax evasion ---
        boolean taxEvasionCaught = false;
        BigDecimal taxEvasionCaughtAmount = BigDecimal.ZERO;
        if (character.isTaxEvasionActive() && !inJail) {
            int evasionLevel = taxEvasionLevel(playerId);
            if (evasionLevel > 0 && taxPaid.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal evaded = taxPaid.multiply(BigDecimal.valueOf(evasionRate(evasionLevel)))
                    .setScale(2, RoundingMode.HALF_UP);
                taxPaid = taxPaid.subtract(evaded);
                character.setCumulativeEvadedTaxes(character.getCumulativeEvadedTaxes().add(evaded));
                if (random.nextDouble() < detectionChance(evasionLevel)) {
                    taxEvasionCaught = true;
                    taxEvasionCaughtAmount = character.getCumulativeEvadedTaxes();
                    character.setTaxEvasionCaughtPending(true);
                    character.setTaxEvasionActive(false);
                    events.add("⚠️ Steuerfahndung! Du wurdest bei der Steuerhinterziehung erwischt!");
                    eventPublisher.publishEvent(new TaxEvasionCaughtEvent(playerId, taxEvasionCaughtAmount));
                }
            }
        }
        if (taxPaid.compareTo(BigDecimal.ZERO) > 0) {
            expenseBreakdown.add(new TurnResultDto.LineItem("Einkommensteuer", taxPaid));
        }

        // --- 4. Monthly expenses ---
        BigDecimal totalExpenses = deductExpenses(playerId, expenseBreakdown);

        // --- 4a. Collection bonuses: expense phase (EXPENSE_REDUCTION) ---
        for (CollectionService.ActiveBonus bonus : collectionBonuses) {
            CollectionBonusApplier applier = bonusApplierMap.get(bonus.bonusType());
            if (applier != null) {
                totalExpenses = applier.modifyExpenses(totalExpenses, bonus.bonusValue());
            }
        }

        totalExpenses = totalExpenses.add(taxPaid);

        // --- 4b. Loan repayments ---
        BigDecimal loanCost = processLoanRepayments(playerId, character, expenseBreakdown, events);
        totalExpenses = totalExpenses.add(loanCost);

        // --- 4c. Health insurance risk ---
        BigDecimal medicalCost = applyHealthInsuranceRisk(playerId, events);
        if (medicalCost.compareTo(BigDecimal.ZERO) > 0) {
            expenseBreakdown.add(new TurnResultDto.LineItem("Arztrechnung (unversichert)", medicalCost));
            totalExpenses = totalExpenses.add(medicalCost);
        }

        // --- 5. Apply net cash change ---
        BigDecimal netChange = grossIncome.subtract(totalExpenses);
        character.setCash(character.getCash().add(netChange).max(BigDecimal.ZERO));

        // --- 5b. Random events ---
        randomEventService.applyRandomEvents(playerId, character, events);

        // --- 6. Needs decay (hunger/energy/happiness) ---
        applyNeedsDecay(character, playerId);

        // --- 6b. Relationship happiness bonus ---
        int happinessBonus = relationshipService.advanceRelationships(playerId, events);
        character.setHappiness(clamp(character.getHappiness() + happinessBonus));

        // --- 6c. Collection bonuses: stat phase (HAPPINESS_BONUS, SCHUFA_BONUS) ---
        for (CollectionService.ActiveBonus bonus : collectionBonuses) {
            CollectionBonusApplier applier = bonusApplierMap.get(bonus.bonusType());
            if (applier != null) {
                applier.applyStats(character, bonus.bonusValue());
            }
        }

        // --- 6d. Critical needs events: Burnout & Depression ---
        applyNeedsCriticalEvents(character, playerId, events);

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

        // --- 9b. Simulate stock prices ---
        stockService.simulatePrices(currentTurn);

        // --- 9c. Recalculate net worth and reload ---
        characterService.recalculateNetWorth(playerId);
        character = characterService.findOrThrow(playerId);

        // --- 10. Save monthly snapshot ---
        monthlySnapshotRepository.save(new MonthlySnapshot(
            playerId, currentTurn, character.getCash(), character.getNetWorth(),
            grossIncome, totalExpenses
        ));

        // --- 11. Publish TurnEndedEvent — TurnEventListener writes EventLog after commit ---
        eventPublisher.publishEvent(new TurnEndedEvent(playerId, currentTurn, events));

        return new TurnResultDto(
            CharacterDto.from(character),
            character.getCurrentTurn(),
            grossIncome,
            taxPaid,
            totalExpenses,
            netChange,
            incomeBreakdown,
            expenseBreakdown,
            events,
            taxEvasionCaught,
            taxEvasionCaughtAmount
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void resolveApplications(Long playerId, int currentTurn, List<String> events) {
        List<JobApplication> pending = jobApplicationRepository.findPendingByPlayerId(playerId);
        for (JobApplication app : pending) {
            if (app.getAppliedAtTurn() >= currentTurn) continue;

            boolean accepted = evaluateApplication(playerId, app);
            app.setStatus(accepted ? "ACCEPTED" : "REJECTED");
            app.setResolvedAtTurn(currentTurn);
            jobApplicationRepository.save(app);

            if (accepted) {
                PlayerJob pj = new PlayerJob(playerId, app.getJob(), currentTurn);
                playerJobRepository.save(pj);
                events.add("Bewerbung angenommen: " + app.getJob().getName());
            } else {
                events.add("Bewerbung abgelehnt: " + app.getJob().getName());
            }

            eventPublisher.publishEvent(new JobApplicationResolvedEvent(
                playerId,
                app.getJob().getId(),
                app.getJob().getName(),
                accepted,
                app.getJob().getSalary()
            ));
        }
    }

    private boolean evaluateApplication(Long playerId, JobApplication app) {
        Job job = app.getJob();

        boolean meetsEducation = meetsEducationRequirement(playerId,
            job.getRequiredEducationType(), job.getRequiredEducationField());

        boolean meetsExperience = true;
        if (job.getRequiredMonthsExperience() > 0) {
            int totalMonths = playerJobRepository.findAllByPlayerId(playerId)
                .stream()
                .mapToInt(PlayerJob::getMonthsWorked)
                .sum();
            meetsExperience = totalMonths >= job.getRequiredMonthsExperience();
        }

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
            pj.setMonthsWorked(pj.getMonthsWorked() + 1);
            playerJobRepository.save(pj);
        }

        if (activeJobs.isEmpty()) {
            events.add("Kein Job — kein Gehalt diesen Monat.");
        }

        return total;
    }

    private BigDecimal deductExpenses(Long playerId, List<TurnResultDto.LineItem> breakdown) {
        boolean hasSelfOccupied = playerRealEstateRepository.findByPlayerId(playerId)
            .stream().anyMatch(pre -> "SELF_OCCUPIED".equals(pre.getMode()));

        BigDecimal total = BigDecimal.ZERO;
        for (MonthlyExpense expense : monthlyExpenseRepository.findActiveByPlayerId(playerId)) {
            if (hasSelfOccupied && "MIETE".equals(expense.getCategory())) {
                breakdown.add(new TurnResultDto.LineItem(expense.getLabel() + " (gespart durch Eigenheim)",
                    BigDecimal.ZERO));
                continue;
            }
            total = total.add(expense.getAmount());
            breakdown.add(new TurnResultDto.LineItem(expense.getLabel(), expense.getAmount()));
        }
        return total;
    }

    private void applyNeedsDecay(GameCharacter character, Long playerId) {
        boolean hasFoodExpense = monthlyExpenseRepository.findActiveByPlayerId(playerId)
            .stream().anyMatch(e -> "ESSEN".equals(e.getCategory()));

        GameConfig.NeedsConfig needs = gameConfig.getNeeds();
        int hungerDecay = hasFoodExpense ? needs.getHungerDecayWithFood() : needs.getHungerDecayBase();
        character.setHunger(clamp(character.getHunger() - hungerDecay));
        character.setEnergy(clamp(character.getEnergy() - needs.getEnergyDecay()));
        character.setHappiness(clamp(character.getHappiness() - needs.getHappinessDecay()));
    }

    private void applyNeedsCriticalEvents(GameCharacter character, Long playerId, List<String> events) {
        GameConfig.BurnoutConfig bo = gameConfig.getBurnout();
        GameConfig.DepressionConfig dep = gameConfig.getDepression();

        if (character.getStress() >= bo.getStressTrigger() && !character.isBurnoutActive()) {
            character.setBurnoutActive(true);
            character.setStress(bo.getStressReset());
            playerJobRepository.findActiveByPlayerId(playerId).forEach(pj -> {
                pj.setActive(false);
                playerJobRepository.save(pj);
            });
            BigDecimal penalty = BigDecimal.valueOf(bo.getHospitalPenalty());
            if (character.getCash().compareTo(penalty) >= 0) {
                character.setCash(character.getCash().subtract(penalty));
            }
            events.add("BURNOUT: Stress war zu hoch — alle Jobs verloren! Krankenhaus-Kosten: -"
                + bo.getHospitalPenalty() + "€. Ruhe dich aus.");
        }

        if (character.getHappiness() <= 0 && character.getDepressionMonthsRemaining() == 0) {
            character.setDepressionMonthsRemaining(dep.getDurationMonths());
            events.add("DEPRESSION eingetreten: Glück auf 0. +" + dep.getStressPerMonth()
                + " Stress/Monat für " + dep.getDurationMonths() + " Monate. Therapie hilft!");
        }

        if (character.getDepressionMonthsRemaining() > 0) {
            character.setStress(Math.min(100, character.getStress() + dep.getStressPerMonth()));
            character.setDepressionMonthsRemaining(character.getDepressionMonthsRemaining() - 1);
            if (character.getDepressionMonthsRemaining() == 0) {
                events.add("Depression überwunden! Weiter so.");
            }
        }

        if (character.isBurnoutActive() && character.getStress() < bo.getRecoveryThreshold()) {
            character.setBurnoutActive(false);
            events.add("Du hast dich vom Burnout erholt. Du kannst wieder arbeiten.");
        }
    }

    private void updateStress(GameCharacter character, Long playerId) {
        List<PlayerJob> activeJobs = playerJobRepository.findActiveByPlayerId(playerId);
        if (activeJobs.isEmpty()) {
            character.setStress(clamp(character.getStress() - 10));
        } else {
            int totalStress = activeJobs.stream().mapToInt(pj -> pj.getJob().getStressPerMonth()).sum();
            character.setStress(clamp(totalStress));
        }
    }

    private void advanceEducation(Long playerId, int currentTurn, List<String> events) {
        educationProgressRepository.findByPlayerId(playerId).ifPresent(ep -> {
            if (ep.getMainStageMonthsRemaining() > 0) {
                ep.setMainStageMonthsRemaining(ep.getMainStageMonthsRemaining() - 1);
                if (ep.getMainStageMonthsRemaining() == 0) {
                    String stageKey = ep.getMainStageField() != null
                        ? ep.getMainStage() + "_" + ep.getMainStageField()
                        : ep.getMainStage();
                    addToCompletedStages(ep, stageKey);
                    events.add("Ausbildung abgeschlossen: " + ep.getMainStage());
                    eventPublisher.publishEvent(new EducationStageCompletedEvent(playerId, stageKey));
                }
            }
            if (ep.getSideCertMonthsRemaining() > 0) {
                ep.setSideCertMonthsRemaining(ep.getSideCertMonthsRemaining() - 1);
                if (ep.getSideCertMonthsRemaining() == 0) {
                    String certKey = ep.getSideCert();
                    addToCompletedStages(ep, certKey);
                    events.add("Weiterbildung abgeschlossen: " + ep.getSideCert());
                    eventPublisher.publishEvent(new SideCertCompletedEvent(playerId, certKey));
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
                eventPublisher.publishEvent(new TravelArrivedEvent(playerId, dest));
            }
        });
    }

    private void generateRandomEvent(Long playerId, int currentTurn, List<String> events) {
        if (random.nextDouble() > 0.20) return;
        List<Collectible> all = collectibleRepository.findAll();
        if (all.isEmpty()) return;
        Collectible c = all.get(random.nextInt(all.size()));
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
     */
    private BigDecimal applyHealthInsuranceRisk(Long playerId, List<String> events) {
        boolean hasKv = monthlyExpenseRepository.findActiveByPlayerId(playerId)
            .stream()
            .anyMatch(e -> "KRANKENVERSICHERUNG".equals(e.getCategory()));

        if (!hasKv && random.nextDouble() < 0.10) {
            int cost = 200 + random.nextInt(1801);
            events.add("Arztrechnung! Ohne Krankenversicherung zahlst du " + cost + " € aus eigener Tasche.");
            return BigDecimal.valueOf(cost);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateRentalIncome(Long playerId,
                                              List<TurnResultDto.LineItem> breakdown) {
        BigDecimal total = BigDecimal.ZERO;
        for (PlayerRealEstate pre : playerRealEstateRepository.findByPlayerId(playerId)) {
            if ("RENTED_OUT".equals(pre.getMode())) {
                BigDecimal rent = pre.getCatalog().getMonthlyRent();
                total = total.add(rent);
                breakdown.add(new TurnResultDto.LineItem("Mieteinnahme: " + pre.getCatalog().getName(), rent));
            }
        }
        return total;
    }

    private BigDecimal processLoanRepayments(Long playerId, GameCharacter character,
                                             List<TurnResultDto.LineItem> breakdown,
                                             List<String> events) {
        BigDecimal total = BigDecimal.ZERO;
        List<PlayerLoan> activeLoans = playerLoanRepository.findActiveByPlayerId(playerId);
        for (PlayerLoan loan : activeLoans) {
            if (character.getCash().add(total.negate()).compareTo(loan.getMonthlyPayment()) >= 0) {
                total = total.add(loan.getMonthlyPayment());
                breakdown.add(new TurnResultDto.LineItem("Kreditrate: " + loan.getLabel(),
                    loan.getMonthlyPayment()));
                loan.setAmountRemaining(loan.getAmountRemaining()
                    .subtract(loan.getMonthlyPayment()).max(BigDecimal.ZERO));
                loan.setTurnsRemaining(loan.getTurnsRemaining() - 1);
                if (loan.getTurnsRemaining() <= 0) {
                    loan.setStatus("PAID_OFF");
                    character.setSchufaScore(LoanService.clampSchufa(character.getSchufaScore() + 5));
                    events.add("Kredit '" + loan.getLabel() + "' vollstaendig zurueckgezahlt! SCHUFA +5");
                    eventPublisher.publishEvent(new LoanPaidOffEvent(playerId, loan.getId(), loan.getLabel()));
                } else {
                    character.setSchufaScore(LoanService.clampSchufa(character.getSchufaScore() + 2));
                }
                playerLoanRepository.save(loan);
            } else {
                loan.setStatus("DEFAULTED");
                character.setSchufaScore(LoanService.clampSchufa(character.getSchufaScore() - 50));
                events.add("Kreditrate nicht bezahlt! SCHUFA -50");
                eventPublisher.publishEvent(
                    new LoanDefaultedEvent(playerId, loan.getId(), loan.getLabel(), loan.getAmountRemaining()));
                playerLoanRepository.save(loan);
            }
        }
        return total;
    }

    int taxEvasionLevel(Long playerId) {
        EducationProgress ep = educationProgressRepository.findByPlayerId(playerId).orElse(null);
        if (ep == null) return 0;
        List<String> completed = Arrays.asList(ep.getCompletedStages());
        List<GameConfig.TaxEvasionConfig.TaxEvasionLevel> levels = gameConfig.getTaxEvasion().getLevels();
        // Iterate in reverse to find the highest matching level
        for (int i = levels.size() - 1; i >= 0; i--) {
            String certKey = "WEITERBILDUNG_" + levels.get(i).getCertSuffix();
            if (completed.contains(certKey)) return i + 1;
        }
        return 0;
    }

    double evasionRate(int level) {
        List<GameConfig.TaxEvasionConfig.TaxEvasionLevel> levels = gameConfig.getTaxEvasion().getLevels();
        if (level < 1 || level > levels.size()) return 0.0;
        return levels.get(level - 1).getEvasionRate();
    }

    double detectionChance(int level) {
        List<GameConfig.TaxEvasionConfig.TaxEvasionLevel> levels = gameConfig.getTaxEvasion().getLevels();
        if (level < 1 || level > levels.size()) return 1.0;
        return levels.get(level - 1).getDetectionChance();
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
