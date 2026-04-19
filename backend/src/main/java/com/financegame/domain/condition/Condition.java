package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

/**
 * A composable, evaluable prerequisite for an action or access gate.
 * Concrete implementations cover cert checks, SCHUFA floors, status checks, etc.
 */
public interface Condition {
    boolean isMet(GameContext context);

    /** Human-readable description of what this condition requires. */
    String describe();
}
