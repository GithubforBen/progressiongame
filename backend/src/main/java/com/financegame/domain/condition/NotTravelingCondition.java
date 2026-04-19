package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

/** Passes when the player is not currently mid-flight to a destination. */
public class NotTravelingCondition implements Condition {

    @Override
    public boolean isMet(GameContext context) {
        return !context.traveling();
    }

    @Override
    public String describe() {
        return "Keine laufende Reise";
    }
}
