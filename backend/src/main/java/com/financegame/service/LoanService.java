package com.financegame.service;

import com.financegame.dto.LoanDto;
import com.financegame.dto.SchufaDto;
import com.financegame.dto.TakeLoanRequest;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerLoan;
import com.financegame.repository.PlayerLoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class LoanService {

    private final PlayerLoanRepository loanRepository;
    private final CharacterService characterService;

    public LoanService(PlayerLoanRepository loanRepository, CharacterService characterService) {
        this.loanRepository = loanRepository;
        this.characterService = characterService;
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

    @Transactional
    public LoanDto takeLoan(Long playerId, TakeLoanRequest req) {
        GameCharacter character = characterService.findOrThrow(playerId);
        int score = character.getSchufaScore();

        if (score < 300) {
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

        // Add loan amount to cash
        characterService.addCash(playerId, req.amount());

        return LoanDto.from(loan);
    }

    // -------------------------------------------------------------------------

    static double interestRateForScore(int score) {
        if (score >= 800) return 0.03;
        if (score >= 600) return 0.05;
        if (score >= 400) return 0.08;
        return 0.12;
    }

    static String schufaLabel(int score) {
        if (score >= 800) return "Ausgezeichnet";
        if (score >= 600) return "Gut";
        if (score >= 400) return "Befriedigend";
        return "Mangelhaft";
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
