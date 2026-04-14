package com.financegame.dto;

import com.financegame.entity.MonthlySnapshot;
import java.math.BigDecimal;

public record SnapshotDto(
    int turn,
    BigDecimal cash,
    BigDecimal netWorth,
    BigDecimal totalIncome,
    BigDecimal totalExpenses
) {
    public static SnapshotDto from(MonthlySnapshot s) {
        return new SnapshotDto(s.getTurn(), s.getCash(), s.getNetWorth(),
                               s.getTotalIncome(), s.getTotalExpenses());
    }
}
