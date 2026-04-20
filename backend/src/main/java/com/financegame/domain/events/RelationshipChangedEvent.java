package com.financegame.domain.events;

import java.time.Instant;

public record RelationshipChangedEvent(
    Long playerId,
    String personId,
    int oldScore,
    int newScore,
    String reason,
    Instant occurredAt
) implements DomainEvent {
    public RelationshipChangedEvent(Long playerId, String personId, int oldScore, int newScore, String reason) {
        this(playerId, personId, oldScore, newScore, reason, Instant.now());
    }
}
