package com.financegame.domain.events;

import java.time.Instant;

/**
 * Fired by TurnService when a player's travel countdown expires and they arrive at their
 * destination country. TravelEventListener reacts by creating a country-specific
 * COLLECTIBLE_SALE ActiveEvent so the player finds a reward on arrival.
 */
public record TravelArrivedEvent(
    Long playerId,
    String countryName,
    Instant occurredAt
) implements DomainEvent {
    public TravelArrivedEvent(Long playerId, String countryName) {
        this(playerId, countryName, Instant.now());
    }
}
