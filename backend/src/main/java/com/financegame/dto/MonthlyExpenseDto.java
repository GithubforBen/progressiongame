package com.financegame.dto;

import com.financegame.entity.MonthlyExpense;
import java.math.BigDecimal;

public record MonthlyExpenseDto(
    Long id,
    String category,
    String label,
    BigDecimal amount,
    boolean active,
    boolean mandatory
) {
    public static MonthlyExpenseDto from(MonthlyExpense e) {
        return new MonthlyExpenseDto(
            e.getId(),
            e.getCategory(),
            e.getLabel(),
            e.getAmount(),
            e.isActive(),
            e.isMandatory()
        );
    }
}
