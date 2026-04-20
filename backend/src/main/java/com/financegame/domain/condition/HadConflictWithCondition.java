package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

public class HadConflictWithCondition implements Condition {

    private final String personId;

    public HadConflictWithCondition(String personId) {
        this.personId = personId;
    }

    @Override
    public boolean isMet(GameContext context) {
        return context.hadConflictsWith().contains(personId);
    }

    @Override
    public String describe() {
        return "Konflikt mit " + personId + " gehabt";
    }
}
