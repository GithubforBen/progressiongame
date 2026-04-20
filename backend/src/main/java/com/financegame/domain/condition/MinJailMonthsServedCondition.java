package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

public class MinJailMonthsServedCondition implements Condition {

    private final int months;

    public MinJailMonthsServedCondition(int months) {
        this.months = months;
    }

    @Override
    public boolean isMet(GameContext context) {
        return context.character().getTotalJailMonthsServed() >= months;
    }

    @Override
    public String describe() {
        return "Mindestens " + months + " Monat(e) im Gefängnis verbracht";
    }
}
