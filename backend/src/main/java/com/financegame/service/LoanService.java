package com.financegame.service;

import com.financegame.config.GameConfig;
import com.financegame.domain.GameContext;
import com.financegame.domain.condition.MinSchufaCondition;
import com.financegame.domain.events.LoanPaidOffEvent;
import com.financegame.domain.events.LoanTakenEvent;
import com.financegame.dto.LoanCapacityDto;
import com.financegame.dto.LoanDto;
import com.financegame.dto.SchufaBreakdownDto;
import com.financegame.dto.SchufaBreakdownDto.SchufaFactor;
import com.financegame.dto.SchufaDto;
import com.financegame.dto.TakeLoanRequest;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerLoan;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.PlayerJobRepository;
import com.financegame.repository.PlayerLoanRepository;
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
import java.util.Set;

@Service
public class LoanService {

    private static final double DEBT_SERVICE_RATIO = 0.40; // max 40% of income for loan payments
    private static final double NET_WORTH_COLLATERAL_RATIO = 0.20; // 20% of net worth as collateral
    private static final BigDecimal MAX_COLLATERAL_WITHOUT_INCOME = new BigDecimal("50000"); // €50k cap without income
    private static final int TOTAL_DEBT_MULTIPLIER = 60; // max 5× annual income (= 60× monthly) in total debt

    private final PlayerLoanRepository loanRepository;
    private final PlayerJobRepository playerJobRepository;
    private final CharacterService characterService;
    private final EducationProgressRepository educationProgressRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GameConfig gameConfig;

    public LoanService(PlayerLoanRepository loanRepository,
                       PlayerJobRepository playerJobRepository,
                       CharacterService characterService,
                       EducationProgressRepository educationProgressRepository,
                       ApplicationEventPublisher eventPublisher,
                       GameConfig gameConfig) {
        this.loanRepository = loanRepository;
        this.playerJobRepository = playerJobRepository;
        this.characterService = characterService;
        this.educationProgressRepository = educationProgressRepository;
        this.eventPublisher = eventPublisher;
        this.gameConfig = gameConfig;
    }

    @Transactional(readOnly = true)
    public List<LoanDto> getLoans(Long playerId) {
        return loanRepository.findByPlayerId(playerId).stream()
            .map(LoanDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SchufaDto getSchufa(Long playerId) {
        GameCharacter character = characterService.findOrThrow(playerId);
        int score = character.getSchufaScore();
        return new SchufaDto(score, schufaLabel(score), interestRateForScore(score));
    }

    @Transactional(readOnly = true)
    public SchufaBreakdownDto getSchufaBreakdown(Long playerId) {
        GameCharacter character = characterService.findOrThrow(playerId);
        int score = character.getSchufaScore();
        List<PlayerLoan> loans = loanRepository.findByPlayerId(playerId);
        List<SchufaFactor> factors = new ArrayList<>();

        factors.add(new SchufaFactor("Basispunkte", 500, "Ausgangswert beim Spielstart"));

        // Education bonus — find the highest-value education prefix the player has completed
        List<String> completed = educationProgressRepository.findByPlayerId(playerId)
            .map(ep -> Arrays.asList(ep.getCompletedStages()))
            .orElse(List.of());
        int eduBonus = 0;
        String eduDetail = "Kein Abschluss";
        for (Map.Entry<String, Integer> entry : gameConfig.getSchufa().getEducationBonuses().entrySet()) {
            String prefix = entry.getKey();
            boolean hasIt = completed.stream()
                .anyMatch(s -> s.equals(prefix) || s.startsWith(prefix + "_"));
            if (hasIt && entry.getValue() > eduBonus) {
                eduBonus = entry.getValue();
                eduDetail = prefix.charAt(0) + prefix.substring(1).toLowerCase().replace("_", " ");
            }
        }
        if (eduBonus > 0) factors.add(new SchufaFactor("Bildungsabschluss", eduBonus, eduDetail));

        // Active loans penalty
        long activeCount = loans.stream().filter(l -> "ACTIVE".equals(l.getStatus())).count();
        if (activeCount > 0) {
            int impact = (int) -(activeCount * 20);
            factors.add(new SchufaFactor("Laufende Kredite", impact,
                activeCount + (activeCount == 1 ? " aktiver Kredit" : " aktive Kredite")));
        }

        // Paid-off loans bonus
        long paidCount = loans.stream().filter(l -> "PAID_OFF".equals(l.getStatus())).count();
        if (paidCount > 0) {
            int impact = (int) (paidCount * 5);
            factors.add(new SchufaFactor("Vollständig abbezahlte Kredite", impact,
                paidCount + (paidCount == 1 ? " Kredit" : " Kredite") + " zurückgezahlt"));
        }

        // Remainder: ongoing payments, defaults, collection bonuses, etc.
        int knownSum = 500 + eduBonus + (int)(activeCount > 0 ? -(activeCount * 20) : 0) + (int)(paidCount * 5);
        int remainder = score - knownSum;
        if (remainder != 0) {
            String detail = remainder > 0 ? "Pünktliche Ratenzahlungen & Sammlungs-Boni" : "Zahlungsausfälle & Strafpunkte";
            factors.add(new SchufaFactor("Weitere Faktoren", remainder, detail));
        }

        return new SchufaBreakdownDto(score, factors);
    }

    @Transactional(readOnly = true)
    public LoanCapacityDto getCapacity(Long playerId, int termMonths) {
        int clampedTerm = Math.max(6, Math.min(360, termMonths));
        GameCharacter character = characterService.findOrThrow(playerId);
        double annualRate = interestRateForScore(character.getSchufaScore());
        return calcCapacity(playerId, character, clampedTerm, annualRate);
    }

    @Transactional
    public LoanDto takeLoan(Long playerId, TakeLoanRequest req) {
        GameCharacter character = characterService.findOrThrow(playerId);
        int score = character.getSchufaScore();

        GameContext ctx = new GameContext(character, List.of(), null, false, 0, Set.of(), Map.of(), Set.of());
        MinSchufaCondition schufaCheck = new MinSchufaCondition(gameConfig.getLoan().getMinSchufa());
        if (!schufaCheck.isMet(ctx)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Kredit abgelehnt: SCHUFA-Score zu niedrig (" + score + "/1000)");
        }
        if (req.amount() == null || req.amount().compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Mindestbetrag: 1.000 €");
        }
        if (req.termMonths() < 6 || req.termMonths() > 360) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Laufzeit muss zwischen 6 und 360 Monaten liegen");
        }

        double annualRate = interestRateForScore(score);

        // Income-based capacity check
        LoanCapacityDto capacity = calcCapacity(playerId, character, req.termMonths(), annualRate);
        if (req.amount().compareTo(capacity.maxLoanAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Kreditrahmen überschritten. Maximum für diese Laufzeit: "
                + capacity.maxLoanAmount().setScale(0, RoundingMode.FLOOR) + " €"
                + " (Einkommen: " + capacity.grossMonthlyIncome().setScale(0, RoundingMode.HALF_UP) + " €/Monat"
                + ", verfügbare Rate: " + capacity.availableMonthlyPayment().setScale(0, RoundingMode.HALF_UP) + " €/Monat)");
        }

        BigDecimal interestRate = BigDecimal.valueOf(annualRate);
        BigDecimal monthlyPayment = calculateAnnuity(req.amount(), annualRate, req.termMonths());

        PlayerLoan loan = new PlayerLoan();
        loan.setPlayerId(playerId);
        loan.setLabel(req.label() != null && !req.label().isBlank() ? req.label() : "Kredit");
        loan.setAmountBorrowed(req.amount());
        loan.setAmountRemaining(req.amount());
        loan.setInterestRate(interestRate);
        loan.setMonthlyPayment(monthlyPayment);
        loan.setTurnsRemaining(req.termMonths());
        loan.setTakenAtTurn(character.getCurrentTurn());
        loan.setStatus("ACTIVE");
        loanRepository.save(loan);

        // SCHUFA penalty for new debt
        characterService.updateSchufaScore(playerId, clampSchufa(score - 20));

        characterService.addCash(playerId, req.amount());

        eventPublisher.publishEvent(new LoanTakenEvent(playerId, loan.getId(), req.amount(), loan.getLabel()));

        return LoanDto.from(loan);
    }

    @Transactional
    public LoanDto payOffLoan(Long playerId, Long loanId) {
        PlayerLoan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kredit nicht gefunden"));
        if (!loan.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nicht dein Kredit");
        }
        if (!"ACTIVE".equals(loan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Kredit ist nicht aktiv (Status: " + loan.getStatus() + ")");
        }
        BigDecimal remaining = loan.getAmountRemaining();
        GameCharacter character = characterService.findOrThrow(playerId);
        if (character.getCash().compareTo(remaining) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Nicht genug Geld. Benötigt: " + remaining + " €, Verfügbar: " + character.getCash() + " €");
        }
        characterService.deductCash(playerId, remaining, "Sofortiger Kreditrückzahlung: " + loan.getLabel());
        loan.setAmountRemaining(BigDecimal.ZERO);
        loan.setTurnsRemaining(0);
        loan.setStatus("PAID_OFF");
        loanRepository.save(loan);
        characterService.updateSchufaScore(playerId, clampSchufa(characterService.findOrThrow(playerId).getSchufaScore() + 5));
        eventPublisher.publishEvent(new LoanPaidOffEvent(playerId, loan.getId(), loan.getLabel()));
        return LoanDto.from(loan);
    }

    // -------------------------------------------------------------------------

    double interestRateForScore(int score) {
        return gameConfig.getLoan().getInterestTiers().stream()
            .filter(t -> score >= t.getMinScore())
            .mapToDouble(GameConfig.LoanConfig.InterestTier::getRate)
            .findFirst()
            .orElse(12.0) / 100.0;
    }

    String schufaLabel(int score) {
        return gameConfig.getLoan().getInterestTiers().stream()
            .filter(t -> score >= t.getMinScore())
            .map(GameConfig.LoanConfig.InterestTier::getLabel)
            .findFirst()
            .orElse("Mangelhaft");
    }

    /**
     * Annuity formula: M = P * (r * (1+r)^n) / ((1+r)^n - 1)
     * where r = monthly rate, n = months
     */
    static BigDecimal calculateAnnuity(BigDecimal principal, double annualRate, int months) {
        double r = annualRate / 12.0;
        if (r == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        double rn = Math.pow(1 + r, months);
        double monthly = principal.doubleValue() * (r * rn) / (rn - 1);
        return BigDecimal.valueOf(monthly).setScale(2, RoundingMode.HALF_UP);
    }

    /** Full capacity breakdown for a given term length and interest rate. */
    private LoanCapacityDto calcCapacity(Long playerId, GameCharacter character,
                                         int termMonths, double annualRate) {
        // 1. Gross monthly income from active jobs
        BigDecimal grossMonthlyIncome = playerJobRepository.findActiveByPlayerId(playerId).stream()
            .map(pj -> pj.getJob().getSalary())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Existing monthly debt payments (active loans only)
        List<PlayerLoan> activeLoans = loanRepository.findActiveByPlayerId(playerId);
        BigDecimal existingMonthlyPayments = activeLoans.stream()
            .map(PlayerLoan::getMonthlyPayment)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Existing total outstanding debt
        BigDecimal existingTotalDebt = activeLoans.stream()
            .map(PlayerLoan::getAmountRemaining)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Max affordable monthly payment (40% rule minus existing commitments)
        BigDecimal maxMonthlyPayment = grossMonthlyIncome
            .multiply(BigDecimal.valueOf(DEBT_SERVICE_RATIO))
            .subtract(existingMonthlyPayments)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_DOWN);

        // 5. Convert payment capacity to max principal (reverse annuity)
        BigDecimal maxByIncome = reverseAnnuity(maxMonthlyPayment, annualRate, termMonths);

        // 6. Net-worth collateral component (20% of net worth, capped without income)
        BigDecimal netWorthCollateral = character.getNetWorth()
            .multiply(BigDecimal.valueOf(NET_WORTH_COLLATERAL_RATIO))
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_DOWN);
        if (grossMonthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            netWorthCollateral = netWorthCollateral.min(MAX_COLLATERAL_WITHOUT_INCOME);
        }

        // 7. Total-debt cap: max 5× annual income outstanding at any time
        BigDecimal totalDebtCap = grossMonthlyIncome.multiply(BigDecimal.valueOf(TOTAL_DEBT_MULTIPLIER));
        BigDecimal maxByDebtCap = totalDebtCap.subtract(existingTotalDebt).max(BigDecimal.ZERO);

        // 8. Effective max = income-based capacity + collateral, then capped by total debt limit
        BigDecimal effectiveMax = maxByIncome.add(netWorthCollateral)
            .min(maxByDebtCap)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_DOWN);

        return new LoanCapacityDto(
            grossMonthlyIncome, existingMonthlyPayments, maxMonthlyPayment,
            effectiveMax, existingTotalDebt, netWorthCollateral
        );
    }

    /** Reverse annuity: given a max monthly payment, returns the max principal. */
    private static BigDecimal reverseAnnuity(BigDecimal maxPayment, double annualRate, int months) {
        if (maxPayment.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        double r = annualRate / 12.0;
        if (r == 0) {
            return maxPayment.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_DOWN);
        }
        double rn = Math.pow(1 + r, months);
        double principal = maxPayment.doubleValue() * (rn - 1) / (r * rn);
        return BigDecimal.valueOf(principal).setScale(2, RoundingMode.HALF_DOWN);
    }

    static int clampSchufa(int v) { return Math.max(0, Math.min(1000, v)); }
}
