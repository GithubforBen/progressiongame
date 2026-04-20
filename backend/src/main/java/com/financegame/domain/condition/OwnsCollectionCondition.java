package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

public class OwnsCollectionCondition implements Condition {

    private final String collectionName;

    public OwnsCollectionCondition(String collectionName) {
        this.collectionName = collectionName;
    }

    @Override
    public boolean isMet(GameContext context) {
        return context.completedCollections().contains(collectionName);
    }

    @Override
    public String describe() {
        return "Sammlung abgeschlossen: " + collectionName;
    }
}
