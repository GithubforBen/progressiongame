package com.financegame.domain.effect;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of all aggregated effects for one player at the time of loading.
 * Compute once per request/turn via PlayerEffectsService.getEffects(playerId) and reuse.
 */
public record PlayerEffects(
    Map<EffectType, Double> totals,
    List<EffectContribution> all
) {

    public static PlayerEffects empty() {
        return new PlayerEffects(Collections.emptyMap(), Collections.emptyList());
    }

    /** Returns the total aggregated value for the given effect type, or 0.0 if none. */
    public double get(EffectType type) {
        return totals.getOrDefault(type, 0.0);
    }

    /** Convenience: returns true when the effect has a non-zero total value. */
    public boolean has(EffectType type) {
        return totals.getOrDefault(type, 0.0) != 0.0;
    }

    /**
     * Build method used by PlayerEffectsService after aggregation.
     * Not for direct use elsewhere.
     */
    public static PlayerEffects of(List<EffectContribution> contributions) {
        Map<EffectType, Double> map = new EnumMap<>(EffectType.class);
        for (EffectContribution c : contributions) {
            map.merge(c.type(), c.value(), Double::sum);
        }
        return new PlayerEffects(Collections.unmodifiableMap(map), Collections.unmodifiableList(contributions));
    }
}
