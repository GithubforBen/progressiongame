package com.financegame.domain.events;

import java.time.Instant;

/**
 * Fired by AuthService after a new player account and all initial game state have
 * been persisted. PlayerRegistrationListener uses this to write a welcome EventLog
 * entry asynchronously.
 */
public record PlayerRegisteredEvent(
    Long playerId,
    String username,
    Instant occurredAt
) implements DomainEvent {
    public PlayerRegisteredEvent(Long playerId, String username) {
        this(playerId, username, Instant.now());
    }
}
