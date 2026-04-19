package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

/** Passes when the player's SCHUFA score meets a minimum threshold. */
public class MinSchufaCondition implements Condition {

    private final int minScore;

    public MinSchufaCondition(int minScore) {
        this.minScore = minScore;
    }

    @Override
    public boolean isMet(GameContext context) {
        return context.character().getSchufaScore() >= minScore;
    }

    @Override
    public String describe() {
        return "SCHUFA-Score mindestens " + minScore + " erforderlich";
    }
}
