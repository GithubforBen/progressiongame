package com.financegame.domain.events;

import java.time.Instant;

/**
 * Fired by TurnService when a player's main education stage countdown reaches zero.
 * stageKey is the composite key stored in completedStages[], e.g. "BACHELOR_INFORMATIK".
 * Listeners can use this to unlock jobs, investments, or real-estate that require this cert.
 */
public record EducationStageCompletedEvent(
    Long playerId,
    String stageKey,
    Instant occurredAt
) implements DomainEvent {
    public EducationStageCompletedEvent(Long playerId, String stageKey) {
        this(playerId, stageKey, Instant.now());
    }
}
