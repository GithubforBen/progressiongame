package com.financegame.dto;

import java.math.BigDecimal;

public record AddExpenseRequest(
    String category,
    String label,
    BigDecimal amount
) {}
