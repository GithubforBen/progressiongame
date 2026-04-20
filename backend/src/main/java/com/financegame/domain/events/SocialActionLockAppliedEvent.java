package com.financegame.domain.events;

import java.time.Instant;

public record SocialActionLockAppliedEvent(
    Long playerId,
    String groupId,
    int untilTurn,
    Instant occurredAt
) implements DomainEvent {
    public SocialActionLockAppliedEvent(Long playerId, String groupId, int untilTurn) {
        this(playerId, groupId, untilTurn, Instant.now());
    }
}
