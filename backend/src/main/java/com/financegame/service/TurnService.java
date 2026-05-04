package com.financegame.service;

import com.financegame.config.GameConfig;
import com.financegame.domain.effect.collection.CollectionBonusApplier;
import com.financegame.domain.events.*;
import com.financegame.dto.CharacterDto;
import com.financegame.dto.TurnResultDto;
import com.financegame.entity.*;
import com.financegame.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final PlayerCollectibleRepository playerCollectibleRepository;
    private final RandomEventService randomEventService;
    private final RelationshipService relationshipService;
    private final PlayerRealEstateRepository playerRealEstateRepository;
    private final PlayerLoanRepository playerLoanRepository;
    private final CollectionService collectionService;
    private final ApplicationEventPublisher eventPublisher;
    private final SocialService socialService;
    private final Map<String, CollectionBonusApplier> bonusApplierMap;
    private final GameConfig gameConfig;
    private final TaxService taxService;
    private final LifestyleService lifestyleService;

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
        PlayerCollectibleRepository playerCollectibleRepository,
        RandomEventService randomEventService,
        RelationshipService relationshipService,
        PlayerRealEstateRepository playerRealEstateRepository,
        PlayerLoanRepository playerLoanRepository,
        CollectionService collectionService,
        ApplicationEventPublisher eventPublisher,
        List<CollectionBonusApplier> bonusAppliers,
        GameConfig gameConfig,
        TaxService taxService,
        SocialService socialService,
        LifestyleService lifestyleService
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
        this.playerCollectibleRepository = playerCollectibleRepository;
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
        this.socialService = socialService;
        this.lifestyleService = lifestyleService;
    }

    @Transactional
    public TurnResultDto endTurn(Long playerId) {
        GameCharacter character = characterService.findOrThrow(playerId);

        if (character.isTaxEvasionCaughtPending()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Du wurdest bei der Steuerhinterziehung erwischt! Löse die Situation zuerst auf.");
        }

        int currentTurn = character.getCurrentTurn();

        List<String> events = new ArrayList<>();
        List<TurnResultDto.LineItem> incomeBreakdown = new ArrayList<>();
        List<TurnResultDto.LineItem> expenseBreakdown = new ArrayList<>();
        List<TurnResultDto.StatChange> stressBreakdown = new ArrayList<>();

        // --- Pre-check: Finanzamt audit for players abusing the multi-job stress bug ---
        {
            List<PlayerJob> auditJobs = playerJobRepository.findActiveByPlayerId(playerId);
            int auditTotalStress = auditJobs.stream().mapToInt(pj -> pj.getJob().getStressPerMonth()).sum();
            if (auditTotalStress >= 100 && !character.isBurnoutActive()
                    && character.getFinanzamtAuditMonthsRemaining() == 0) {
                BigDecimal seized = character.getCash()
                    .multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP)
                    .max(BigDecimal.ZERO);
                character.setCash(character.getCash().subtract(seized));
                character.setFinanzamtAuditMonthsRemaining(14);
                character.setTaxEvasionActive(false);
                events.add("🔍 ÜBERPRÜFUNG DURCH DAS FINANZAMT: Verdacht auf Steuervergehen durch "
                    + "mehrfache Beschäftigung! " + seized + " € Vermögen beschlagnahmt. "
                    + "Steuerhinterziehung für 14 Monate gesperrt.");
            }
        }

        // --- 0. Jail / Exile ticks ---
        boolean inJail = character.getJailMonthsRemaining() > 0;
        if (inJail) {
            character.setJailMonthsRemaining(character.getJailMonthsRemaining() - 1);
            character.setStress(Math.min(100, character.getStress() + 15));
            stressBreakdown.add(new TurnResultDto.StatChange("Haft", +15));
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
                boolean hasRechtsschutz = monthlyExpenseRepository.findActiveByPlayerId(playerId)
                    .stream().anyMatch(e -> "RECHTSSCHUTZ".equals(e.getCategory()));
                boolean hasJet = lifestyleService.getOwnedCatalogItems(playerId)
                    .stream().anyMatch(c -> c.isTaxEvasionBoost());
                double baseChance = detectionChance(evasionLevel);
                double finalChance = baseChance;
                if (hasRechtsschutz) finalChance *= 0.70;
                if (hasJet) finalChance *= 0.85;
                if (random.nextDouble() < finalChance) {
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

        // --- 5. Apply net cash change (allow overdraft down to -5000) ---
        BigDecimal netChange = grossIncome.subtract(totalExpenses);
        BigDecimal newCash = character.getCash().add(netChange)
            .max(CharacterService.OVERDRAFT_LIMIT);
        character.setCash(newCash);

        // --- 5a. Overdraft interest & Schufa penalty ---
        if (newCash.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal interest = newCash.abs()
                .multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
            character.setCash(character.getCash().subtract(interest).max(CharacterService.OVERDRAFT_LIMIT));
            expenseBreakdown.add(new TurnResultDto.LineItem("Überziehungszinsen (2%)", interest));
            totalExpenses = totalExpenses.add(interest);
            character.setSchufaScore(LoanService.clampSchufa(character.getSchufaScore() - 10));
            events.add("Kontoüberziehung! Schufa -10, Zinsen: " + interest + " €");
        }

        // --- 7. Stress-Basis setzen (Jobs) — muss VOR allen Reduktionen laufen ---
        updateStress(character, playerId, events, stressBreakdown);

        // --- 5b. Random events ---
        int stressPreEvents = character.getStress();
        randomEventService.applyRandomEvents(playerId, character, events);
        int stressEventDelta = character.getStress() - stressPreEvents;
        if (stressEventDelta != 0) {
            stressBreakdown.add(new TurnResultDto.StatChange("Zufallsereignis", stressEventDelta));
        }

        // --- 6. Needs decay (hunger/energy/happiness) + Lifestyle/Krankenkasse ---
        applyNeedsDecay(character, playerId, stressBreakdown);

        // --- 6b. Advance social relationships + apply stat boosts ---
        socialService.advanceSocials(playerId);
        int stressPreSocial = character.getStress();
        for (SocialService.ActiveBoostDto boost : socialService.getActiveBoosts(playerId)) {
            switch (boost.type()) {
                case "HAPPINESS_PER_TURN" -> character.setHappiness(clamp(character.getHappiness() + (int) boost.totalValue()));
                case "STRESS_REDUCTION_PER_TURN" -> character.setStress(clamp(character.getStress() - (int) boost.totalValue()));
                case "ENERGY_BONUS_PER_TURN" -> character.setEnergy(clamp(character.getEnergy() + (int) boost.totalValue()));
                case "SCHUFA_BONUS_MONTHLY" -> character.setSchufaScore(character.getSchufaScore() + (int) boost.totalValue());
                default -> {} // other boost types applied elsewhere
            }
        }
        int stressSocialDelta = character.getStress() - stressPreSocial;
        if (stressSocialDelta != 0) {
            stressBreakdown.add(new TurnResultDto.StatChange("Beziehungs-Boosts", stressSocialDelta));
        }

        // --- 6c. Collection bonuses: stat phase (HAPPINESS_BONUS, SCHUFA_BONUS) ---
        int stressPreCollection = character.getStress();
        for (CollectionService.ActiveBonus bonus : collectionBonuses) {
            CollectionBonusApplier applier = bonusApplierMap.get(bonus.bonusType());
            if (applier != null) {
                applier.applyStats(character, bonus.bonusValue());
            }
        }
        int stressCollectionDelta = character.getStress() - stressPreCollection;
        if (stressCollectionDelta != 0) {
            stressBreakdown.add(new TurnResultDto.StatChange("Sammlungs-Boni", stressCollectionDelta));
        }

        // --- 6d. Critical needs events: Burnout & Depression ---
        int stressPreCritical = character.getStress();
        applyNeedsCriticalEvents(character, playerId, events);
        int stressCriticalDelta = character.getStress() - stressPreCritical;
        if (stressCriticalDelta != 0) {
            stressBreakdown.add(new TurnResultDto.StatChange("Burnout/Depression", stressCriticalDelta));
        }

        // --- 8. Increment months worked & education ---
        advanceEducation(playerId, currentTurn, events);

        // --- 8c. Check travel arrival ---
        checkTravelArrival(playerId, currentTurn, events);

        // --- 8d. Expire old events, generate new ones ---
        activeEventRepository.deleteExpiredForPlayer(playerId, currentTurn);
        generateRandomEvent(playerId, currentTurn, events);

        // --- 8e. Schufa passive regeneration toward 500 ---
        int schufa = character.getSchufaScore();
        int drift = 0;
        if (schufa < 500) drift = 1;
        else if (schufa > 500) drift = -1;
        if (character.getNetWorth() != null
                && character.getNetWorth().compareTo(new BigDecimal("1000000")) > 0) {
            drift += (schufa < 500) ? 2 : -2;
        }
        if (drift != 0) {
            character.setSchufaScore(LoanService.clampSchufa(schufa + drift));
        }

        // --- 8f. Decrement Finanzamt audit counter ---
        if (character.getFinanzamtAuditMonthsRemaining() > 0) {
            character.setFinanzamtAuditMonthsRemaining(character.getFinanzamtAuditMonthsRemaining() - 1);
            if (character.getFinanzamtAuditMonthsRemaining() == 0) {
                events.add("✅ Finanzamt-Überprüfung abgeschlossen. Steuerhinterziehung wieder möglich.");
            }
        }

        // --- 9. Advance turn counter & persist character ---
        character.setCurrentTurn(currentTurn + 1);
        characterRepository.save(character);

        // --- 9b. Simulate stock prices ---
        stockService.simulatePrices(playerId, currentTurn);

        // --- 9c. Recalculate net worth and reload ---
        characterService.recalculateNetWorth(playerId);
        character = characterService.findOrThrow(playerId);

        // --- 9d. Check victory condition ---
        checkVictoryCondition(playerId, character, events);

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
            taxEvasionCaughtAmount,
            stressBreakdown
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

        // Lifestyle item monthly costs (e.g., Privat Jet fuel)
        for (var item : lifestyleService.getOwnedCatalogItems(playerId)) {
            if (item.getMonthlyCost().compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(item.getMonthlyCost());
                breakdown.add(new TurnResultDto.LineItem(item.getName() + " (Betriebskosten)", item.getMonthlyCost()));
            }
        }

        return total;
    }

    private void applyNeedsDecay(GameCharacter character, Long playerId,
                                 List<TurnResultDto.StatChange> stressBreakdown) {
        List<MonthlyExpense> activeExpenses = monthlyExpenseRepository.findActiveByPlayerId(playerId);
        boolean hasFoodExpense = activeExpenses.stream().anyMatch(e -> "ESSEN".equals(e.getCategory()));

        GameConfig.NeedsConfig needs = gameConfig.getNeeds();
        int hungerDecay = hasFoodExpense ? needs.getHungerDecayWithFood() : needs.getHungerDecayBase();
        character.setHunger(clamp(character.getHunger() - hungerDecay));
        character.setEnergy(clamp(character.getEnergy() - needs.getEnergyDecay()));
        character.setHappiness(clamp(character.getHappiness() - needs.getHappinessDecay()));

        // Krankenkasse tier stress reduction
        activeExpenses.stream()
            .filter(e -> "KRANKENVERSICHERUNG".equals(e.getCategory()))
            .findFirst().ifPresent(kv -> {
                int reduction = 0;
                if (kv.getAmount().compareTo(new BigDecimal("400")) >= 0) reduction = 10;
                else if (kv.getAmount().compareTo(new BigDecimal("250")) >= 0) reduction = 5;
                if (reduction > 0) {
                    character.setStress(clamp(character.getStress() - reduction));
                    stressBreakdown.add(new TurnResultDto.StatChange(
                        "Krankenversicherung (" + kv.getAmount() + " €/Monat)", -reduction));
                }
            });

        // Lifestyle item stress reduction (logged per item)
        lifestyleService.getOwnedCatalogItems(playerId).forEach(item -> {
            if (item.getStressReductionMonth() > 0) {
                int before = character.getStress();
                character.setStress(clamp(character.getStress() - item.getStressReductionMonth()));
                int actual = character.getStress() - before;
                if (actual != 0) {
                    stressBreakdown.add(new TurnResultDto.StatChange(item.getName(), actual));
                }
            }
        });
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
            monthlyExpenseRepository.findByPlayerId(playerId).stream()
                .filter(e -> "THERAPIE".equals(e.getCategory()))
                .forEach(e -> monthlyExpenseRepository.delete(e.getId()));
            events.add("Du hast dich vom Burnout erholt. Therapie nicht mehr notwendig. Du kannst wieder arbeiten.");
        }
    }

    private void updateStress(GameCharacter character, Long playerId,
                              List<String> events, List<TurnResultDto.StatChange> stressBreakdown) {
        List<PlayerJob> activeJobs = playerJobRepository.findActiveByPlayerId(playerId);
        if (activeJobs.isEmpty()) {
            int prev = character.getStress();
            character.setStress(clamp(character.getStress() - 10));
            stressBreakdown.add(new TurnResultDto.StatChange("Keine Jobs (Entspannung)", character.getStress() - prev));
            return;
        }
        int totalStress = activeJobs.stream().mapToInt(pj -> pj.getJob().getStressPerMonth()).sum();
        if (totalStress >= 100 && !character.isBurnoutActive()) {
            character.setBurnoutActive(true);
            GameConfig.BurnoutConfig bo = gameConfig.getBurnout();
            int prev = character.getStress();
            character.setStress(bo.getStressReset());
            BigDecimal monthlyIncome = activeJobs.stream()
                .map(pj -> pj.getJob().getSalary())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            activeJobs.forEach(pj -> {
                pj.setActive(false);
                playerJobRepository.save(pj);
            });
            BigDecimal penalty = BigDecimal.valueOf(bo.getHospitalPenalty());
            if (character.getCash().compareTo(penalty) >= 0) {
                character.setCash(character.getCash().subtract(penalty));
            }
            stressBreakdown.add(new TurnResultDto.StatChange(
                "BURNOUT — Jobs erzeugten " + totalStress + "/100 Stress", character.getStress() - prev));
            boolean hasTherapie = monthlyExpenseRepository.findByPlayerId(playerId).stream()
                .anyMatch(e -> "THERAPIE".equals(e.getCategory()));
            if (!hasTherapie && monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
                MonthlyExpense therapie = new MonthlyExpense();
                therapie.setPlayerId(playerId);
                therapie.setCategory("THERAPIE");
                therapie.setLabel("Therapie (Burnout-Pflicht)");
                therapie.setAmount(monthlyIncome.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP));
                therapie.setActive(true);
                therapie.setMandatory(true);
                monthlyExpenseRepository.save(therapie);
                events.add("BURNOUT: Zu viele Jobs — maximaler Stresslevel überschritten! "
                    + "Alle Jobs verloren. Krankenhaus: -" + bo.getHospitalPenalty()
                    + " €. Pflicht-Therapie hinzugefügt: " + therapie.getAmount() + " €/Monat.");
            } else {
                events.add("BURNOUT: Zu viele Jobs — maximaler Stresslevel überschritten! "
                    + "Alle Jobs verloren. Krankenhaus: -" + bo.getHospitalPenalty() + " €.");
            }
        } else if (!character.isBurnoutActive()) {
            String jobNames = activeJobs.stream()
                .map(pj -> pj.getJob().getName())
                .collect(Collectors.joining(", "));
            int prev = character.getStress();
            character.setStress(clamp(totalStress));
            stressBreakdown.add(new TurnResultDto.StatChange(
                "Jobs (" + jobNames + "): " + totalStress + "/100", character.getStress() - prev));
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

    private static final BigDecimal VICTORY_NET_WORTH = new BigDecimal("500000000000"); // 500 billion
    private static final java.util.Set<String> VICTORY_ITEMS =
        java.util.Set.of("SCHLOSS", "SUPERCAR", "SUPER_YACHT", "SPACE_STATION");

    private void checkVictoryCondition(Long playerId, GameCharacter character, List<String> events) {
        if (character.isVictoryAchieved()) return;
        if (character.getNetWorth().compareTo(VICTORY_NET_WORTH) < 0) return;

        java.util.Set<String> ownedLifestyle = lifestyleService.getOwnedCatalogItems(playerId)
            .stream().map(com.financegame.entity.LifestyleItemCatalog::getId)
            .collect(java.util.stream.Collectors.toSet());
        if (!ownedLifestyle.containsAll(VICTORY_ITEMS)) return;

        // All collectibles must be owned
        java.util.Set<Long> allCollectibleIds = collectibleRepository.findAll()
            .stream().map(com.financegame.entity.Collectible::getId)
            .collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> ownedCollectibleIds = playerCollectibleRepository.findByPlayerId(playerId)
            .stream().map(com.financegame.entity.PlayerCollectible::getCollectibleId)
            .collect(java.util.stream.Collectors.toSet());
        if (!ownedCollectibleIds.containsAll(allCollectibleIds)) return;

        character.setVictoryAchieved(true);
        if (character.getNetWorth().compareTo(character.getPersonalBestNetWorth()) > 0) {
            character.setPersonalBestNetWorth(character.getNetWorth());
        }
        characterRepository.save(character);
        events.add("🏆 SIEG! Du hast das Spiel gewonnen! Nettovermögen: " + character.getNetWorth() + " €");
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
