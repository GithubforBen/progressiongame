package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

/** Passes when the player has completed a specific education cert or stage. */
public class HasCertCondition implements Condition {

    private final String certKey;

    public HasCertCondition(String certKey) {
        this.certKey = certKey;
    }

    @Override
    public boolean isMet(GameContext context) {
        return context.completedEducationStages().contains(certKey);
    }

    @Override
    public String describe() {
        return "Weiterbildung erforderlich: " + certKey;
    }
}
