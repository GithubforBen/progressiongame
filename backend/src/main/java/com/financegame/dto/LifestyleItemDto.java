package com.financegame.dto;

import com.financegame.entity.LifestyleItemCatalog;
import java.math.BigDecimal;

public record LifestyleItemDto(
    String id,
    String name,
    String icon,
    BigDecimal cost,
    BigDecimal monthlyCost,
    int stressReductionMonth,
    boolean taxEvasionBoost,
    boolean unlocksBillionaire,
    String description,
    boolean owned
) {
    public static LifestyleItemDto from(LifestyleItemCatalog c, boolean owned) {
        return new LifestyleItemDto(
            c.getId(), c.getName(), c.getIcon(), c.getCost(), c.getMonthlyCost(),
            c.getStressReductionMonth(), c.isTaxEvasionBoost(), c.isUnlocksBillionaire(),
            c.getDescription(), owned
        );
    }
}
