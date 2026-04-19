package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by CollectibleService and CollectionService when a player successfully buys
 * a collectible. CollectionEventListener uses this to check whether the purchase
 * completes a collection and fires CollectionCompletedEvent if so.
 */
public record CollectiblePurchasedEvent(
    Long playerId,
    Long collectibleId,
    String collectibleName,
    String collectionName,
    BigDecimal price,
    Instant occurredAt
) implements DomainEvent {
    public CollectiblePurchasedEvent(Long playerId, Long collectibleId, String collectibleName,
                                     String collectionName, BigDecimal price) {
        this(playerId, collectibleId, collectibleName, collectionName, price, Instant.now());
    }
}
