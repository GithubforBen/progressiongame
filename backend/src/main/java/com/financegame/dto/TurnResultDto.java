package com.financegame.dto;

import java.math.BigDecimal;
import java.util.List;

public record TurnResultDto(
    CharacterDto character,
    int newTurn,
    BigDecimal grossIncome,
    BigDecimal taxPaid,
    BigDecimal totalExpenses,
    BigDecimal netChange,
    List<LineItem> incomeBreakdown,
    List<LineItem> expenseBreakdown,
    List<String> events
) {
    public record LineItem(String label, BigDecimal amount) {}
}
