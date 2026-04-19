package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

/** Passes when the player is not currently in jail. */
public class NotInJailCondition implements Condition {

    @Override
    public boolean isMet(GameContext context) {
        return context.character().getJailMonthsRemaining() == 0;
    }

    @Override
    public String describe() {
        return "Nicht in Haft";
    }
}
