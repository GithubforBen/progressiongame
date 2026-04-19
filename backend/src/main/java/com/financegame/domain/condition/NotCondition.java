package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

/** Inverts another condition (logical NOT). */
public class NotCondition implements Condition {

    private final Condition inner;

    public NotCondition(Condition inner) {
        this.inner = inner;
    }

    @Override
    public boolean isMet(GameContext context) {
        return !inner.isMet(context);
    }

    @Override
    public String describe() {
        return "Nicht: " + inner.describe();
    }
}
