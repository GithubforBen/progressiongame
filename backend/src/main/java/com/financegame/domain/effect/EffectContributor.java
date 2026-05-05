package com.financegame.domain.effect;

import java.util.List;

/**
 * Implemented by each effect source (social relationships, collections, lifestyle items).
 * Mark implementations with @Component — PlayerEffectsService auto-discovers them all.
 *
 * To add a new effect source: create a @Component class in this package implementing this
 * interface. No other changes are needed.
 */
public interface EffectContributor {
    List<EffectContribution> getContributions(Long playerId);
}
