package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

public class HasRelationshipScoreCondition implements Condition {

    private final String personId;
    private final int minScore;

    public HasRelationshipScoreCondition(String personId, int minScore) {
        this.personId = personId;
        this.minScore = minScore;
    }

    @Override
    public boolean isMet(GameContext context) {
        return context.relationshipScores().getOrDefault(personId, 0) >= minScore;
    }

    @Override
    public String describe() {
        return "Beziehung zu " + personId + " ≥ " + minScore;
    }
}
