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
    boolean depressionReduction,
    int cooldownTurns,           // how many months must pass between uses
    int cooldownRemainingTurns   // 0 = available now, >0 = months until available
) {
    public static NeedsItemDto from(NeedsItem n) {
        return from(n, 0);
    }

    public static NeedsItemDto from(NeedsItem n, int cooldownRemaining) {
        return new NeedsItemDto(
            n.getId(), n.getName(), n.getPrice(),
            n.getHungerEffect(), n.getEnergyEffect(),
            n.getHappinessEffect(), n.getStressEffect(),
            n.isDepressionReduction(),
            n.getCooldownTurns(), cooldownRemaining
        );
    }
}
