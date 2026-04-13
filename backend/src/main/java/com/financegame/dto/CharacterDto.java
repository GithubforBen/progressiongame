package com.financegame.dto;

import com.financegame.entity.GameCharacter;
import java.math.BigDecimal;

public record CharacterDto(
    Long id,
    BigDecimal cash,
    BigDecimal netWorth,
    int stress,
    int hunger,
    int energy,
    int happiness,
    int currentTurn
) {
    public static CharacterDto from(GameCharacter c) {
        return new CharacterDto(
            c.getId(),
            c.getCash(),
            c.getNetWorth(),
            c.getStress(),
            c.getHunger(),
            c.getEnergy(),
            c.getHappiness(),
            c.getCurrentTurn()
        );
    }
}
