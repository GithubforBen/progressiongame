package com.financegame.dto;

import com.financegame.entity.PlayerLoan;

import java.math.BigDecimal;

public record LoanDto(
    Long id,
    String label,
    BigDecimal amountBorrowed,
    BigDecimal amountRemaining,
    BigDecimal interestRate,
    BigDecimal monthlyPayment,
    int turnsRemaining,
    int takenAtTurn,
    String status
) {
    public static LoanDto from(PlayerLoan l) {
        return new LoanDto(
            l.getId(),
            l.getLabel(),
            l.getAmountBorrowed(),
            l.getAmountRemaining(),
            l.getInterestRate(),
            l.getMonthlyPayment(),
            l.getTurnsRemaining(),
            l.getTakenAtTurn(),
            l.getStatus()
        );
    }
}
