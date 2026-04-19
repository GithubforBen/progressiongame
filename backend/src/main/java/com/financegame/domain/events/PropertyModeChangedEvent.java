package com.financegame.domain.events;

import java.time.Instant;

/**
 * Fired by RealEstateService when a player switches a property between
 * SELF_OCCUPIED and RENTED_OUT. Useful for reacting to occupancy changes,
 * e.g. adjusting monthly expense calculations or notifying NPC tenants.
 */
public record PropertyModeChangedEvent(
    Long playerId,
    Long propertyId,
    String newMode,
    Instant occurredAt
) implements DomainEvent {
    public PropertyModeChangedEvent(Long playerId, Long propertyId, String newMode) {
        this(playerId, propertyId, newMode, Instant.now());
    }
}
