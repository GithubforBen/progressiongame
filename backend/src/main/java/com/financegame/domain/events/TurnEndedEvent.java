package com.financegame.domain.events;

import java.time.Instant;
import java.util.List;

/**
 * Fired at the end of TurnService.endTurn() after all business logic has completed
 * and the character state has been persisted. Carries the full list of human-readable
 * event messages so that TurnEventListener can write them to EventLog without
 * TurnService needing to hold an EventLogRepository dependency.
 */
public record TurnEndedEvent(
    Long playerId,
    int turnNumber,
    List<String> eventMessages,
    Instant occurredAt
) implements DomainEvent {
    public TurnEndedEvent(Long playerId, int turnNumber, List<String> eventMessages) {
        this(playerId, turnNumber, List.copyOf(eventMessages), Instant.now());
    }
}
