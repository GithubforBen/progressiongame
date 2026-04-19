package com.financegame.domain.effect.collection;

import com.financegame.entity.GameCharacter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SchufaBonusApplier implements CollectionBonusApplier {

    @Override
    public String getBonusType() { return "SCHUFA_BONUS"; }

    @Override
    public void applyStats(GameCharacter character, BigDecimal bonusValue) {
        int newScore = character.getSchufaScore() + bonusValue.intValue();
        character.setSchufaScore(Math.max(0, Math.min(1000, newScore)));
    }
}
