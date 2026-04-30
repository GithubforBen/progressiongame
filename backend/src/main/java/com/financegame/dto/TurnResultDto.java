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
    List<String> events,
    boolean taxEvasionCaught,
    BigDecimal taxEvasionCaughtAmount,
    List<StatChange> stressBreakdown
) {
    public record LineItem(String label, BigDecimal amount) {}
    /** Represents one source of stress change this month. delta > 0 = more stress, delta < 0 = less stress. */
    public record StatChange(String label, int delta) {}
}
