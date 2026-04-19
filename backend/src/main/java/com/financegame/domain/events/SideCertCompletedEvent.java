package com.financegame.domain.events;

import java.time.Instant;

/**
 * Fired by TurnService when a side certification countdown reaches zero.
 * certKey matches the key stored in completedStages[], e.g. "WEITERBILDUNG_STEUERHINTERZIEHUNG_1".
 * Listeners may use this to unlock special features gated behind the cert.
 */
public record SideCertCompletedEvent(
    Long playerId,
    String certKey,
    Instant occurredAt
) implements DomainEvent {
    public SideCertCompletedEvent(Long playerId, String certKey) {
        this(playerId, certKey, Instant.now());
    }
}
