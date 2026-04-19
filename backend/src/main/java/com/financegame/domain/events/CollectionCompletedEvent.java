package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by CollectionEventListener (as a secondary event) when buying a collectible
 * completes the last item slot in a collection. Other modules can listen to this event
 * to unlock features, award achievements, or apply one-time bonuses.
 */
public record CollectionCompletedEvent(
    Long playerId,
    String collectionName,
    String displayName,
    String bonusType,
    BigDecimal bonusValue,
    Instant occurredAt
) implements DomainEvent {
    public CollectionCompletedEvent(Long playerId, String collectionName, String displayName,
                                    String bonusType, BigDecimal bonusValue) {
        this(playerId, collectionName, displayName, bonusType, bonusValue, Instant.now());
    }
}
