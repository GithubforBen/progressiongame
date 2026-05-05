package com.financegame.domain.effect;

/**
 * A single contribution to a player's effects from one source.
 *
 * @param type        the canonical effect type
 * @param value       the numerical contribution (additive with other contributions of the same type)
 * @param sourceLabel category label shown to the player, e.g. "Beziehung", "Sammlung", "Lifestyle"
 * @param sourceDetail specific source name, e.g. person name, collection name, item name
 */
public record EffectContribution(
    EffectType type,
    double value,
    String sourceLabel,
    String sourceDetail
) {}
