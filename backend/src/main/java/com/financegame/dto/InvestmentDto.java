package com.financegame.dto;

import com.financegame.entity.Investment;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record InvestmentDto(
    Long id,
    String type,
    String name,
    Long stockId,
    BigDecimal quantity,
    BigDecimal amountInvested,
    BigDecimal currentValue,
    BigDecimal gainLoss,
    BigDecimal gainLossPct,
    int acquiredAtTurn
) {
    public static InvestmentDto from(Investment inv) {
        BigDecimal gainLoss = inv.getCurrentValue().subtract(inv.getAmountInvested());
        BigDecimal gainLossPct = null;
        if (inv.getAmountInvested().compareTo(BigDecimal.ZERO) != 0) {
            gainLossPct = gainLoss
                .divide(inv.getAmountInvested(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }
        return new InvestmentDto(
            inv.getId(),
            inv.getType(),
            inv.getName(),
            inv.getStockId(),
            inv.getQuantity(),
            inv.getAmountInvested(),
            inv.getCurrentValue(),
            gainLoss,
            gainLossPct,
            inv.getAcquiredAtTurn()
        );
    }
}
