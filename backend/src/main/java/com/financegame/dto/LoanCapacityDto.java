package com.financegame.dto;

import java.math.BigDecimal;

public record LoanCapacityDto(
    BigDecimal grossMonthlyIncome,
    BigDecimal existingMonthlyPayments,
    BigDecimal availableMonthlyPayment,  // income * 0.40 - existing payments
    BigDecimal maxLoanAmount,            // max new loan for the requested term
    BigDecimal existingTotalDebt,        // sum of amountRemaining on active loans
    BigDecimal netWorthCollateral        // 20% of net worth (collateral component)
) {}
