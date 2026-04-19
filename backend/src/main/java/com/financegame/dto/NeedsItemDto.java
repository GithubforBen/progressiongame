package com.financegame.dto;

import com.financegame.entity.NeedsItem;

import java.math.BigDecimal;

public record NeedsItemDto(
    String id,
    String name,
    BigDecimal price,
    int hungerEffect,
    int energyEffect,
    int happinessEffect,
    int stressEffect,
    boolean depressionReduction
) {
    public static NeedsItemDto from(NeedsItem n) {
        return new NeedsItemDto(
            n.getId(), n.getName(), n.getPrice(),
            n.getHungerEffect(), n.getEnergyEffect(),
            n.getHappinessEffect(), n.getStressEffect(),
            n.isDepressionReduction()
        );
    }
}
