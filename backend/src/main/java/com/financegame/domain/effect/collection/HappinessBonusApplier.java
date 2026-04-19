package com.financegame.domain.effect.collection;

import com.financegame.entity.GameCharacter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HappinessBonusApplier implements CollectionBonusApplier {

    @Override
    public String getBonusType() { return "HAPPINESS_BONUS"; }

    @Override
    public void applyStats(GameCharacter character, BigDecimal bonusValue) {
        character.setHappiness(clamp(character.getHappiness() + bonusValue.intValue()));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
