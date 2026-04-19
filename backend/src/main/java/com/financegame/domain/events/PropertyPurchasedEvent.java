package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by RealEstateService when a player successfully purchases a property.
 * Allows listeners to log the purchase or apply cross-module effects such as
 * unlocking travel-related bonuses tied to real-estate ownership.
 */
public record PropertyPurchasedEvent(
    Long playerId,
    Long catalogId,
    String propertyName,
    BigDecimal price,
    Instant occurredAt
) implements DomainEvent {
    public PropertyPurchasedEvent(Long playerId, Long catalogId, String propertyName, BigDecimal price) {
        this(playerId, catalogId, propertyName, price, Instant.now());
    }
}
