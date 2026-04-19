package com.financegame.domain.events;

import java.time.Instant;

/**
 * Fired by TravelService when a player books a flight and departs from their current location.
 * Can be used to restrict actions during transit (e.g. no job applications while traveling).
 */
public record TravelDepartedEvent(
    Long playerId,
    String destinationCountry,
    int arriveAtTurn,
    Instant occurredAt
) implements DomainEvent {
    public TravelDepartedEvent(Long playerId, String destinationCountry, int arriveAtTurn) {
        this(playerId, destinationCountry, arriveAtTurn, Instant.now());
    }
}
