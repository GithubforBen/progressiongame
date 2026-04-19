package com.financegame.domain.effect;

import com.financegame.domain.GameContext;

/**
 * A side-effect that mutates player state (cash, stats, SCHUFA, etc.).
 * Implementations are composable and can be stored as data.
 */
public interface Effect {
    void apply(GameContext context);
    String describe();
}
