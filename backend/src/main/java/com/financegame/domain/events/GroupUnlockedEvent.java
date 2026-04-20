package com.financegame.domain.events;

import java.time.Instant;

public record GroupUnlockedEvent(
    Long playerId,
    String groupId,
    String groupName,
    Instant occurredAt
) implements DomainEvent {
    public GroupUnlockedEvent(Long playerId, String groupId, String groupName) {
        this(playerId, groupId, groupName, Instant.now());
    }
}
