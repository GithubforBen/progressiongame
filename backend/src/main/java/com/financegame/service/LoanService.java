package com.financegame.service;

import com.financegame.config.GameConfig;
import com.financegame.domain.GameContext;
import com.financegame.domain.condition.MinSchufaCondition;
import com.financegame.domain.events.LoanTakenEvent;
import com.financegame.dto.LoanDto;
import com.financegame.dto.SchufaBreakdownDto;
import com.financegame.dto.SchufaBreakdownDto.SchufaFactor;
import com.financegame.dto.SchufaDto;
import com.financegame.dto.TakeLoanRequest;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerLoan;
import com.financegame.repository.EducationProgressRepository;
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

@Service
public class LoanService {

    private final PlayerLoanRepository loanRepository;
    private final CharacterService characterService;
    private final EducationProgressRepository educationProgressRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GameConfig gameConfig;

    public LoanService(PlayerLoanRepository loanRepository,
                       CharacterService characterService,
                       EducationProgressRepository educationProgressRepository,
                       ApplicationEventPublisher eventPublisher,
                       GameConfig gameConfig) {
        this.loanRepository = loanRepository;
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

    @Transactional
    public LoanDto takeLoan(Long playerId, TakeLoanRequest req) {
        GameCharacter character = characterService.findOrThrow(playerId);
        int score = character.getSchufaScore();

        GameContext ctx = new GameContext(character, List.of(), null, false, 0);
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

    static int clampSchufa(int v) { return Math.max(0, Math.min(1000, v)); }
}
