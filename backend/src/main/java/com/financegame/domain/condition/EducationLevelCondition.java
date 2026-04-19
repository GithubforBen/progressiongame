package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

/**
 * Passes when the player has completed any stage whose key starts with the given prefix.
 * E.g., prefix "BACHELOR" matches "BACHELOR_INFORMATIK", "BACHELOR_MEDIZIN", etc.
 */
public class EducationLevelCondition implements Condition {

    private final String prefix;

    public EducationLevelCondition(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean isMet(GameContext context) {
        return context.completedEducationStages().stream()
            .anyMatch(stage -> stage.equals(prefix) || stage.startsWith(prefix + "_"));
    }

    @Override
    public String describe() {
        return "Bildungsabschluss erforderlich: " + prefix;
    }
}
