package com.financegame.domain.events;

import java.time.Instant;

/**
 * Marker interface for all domain events in FinanzLeben.
 * Every event must carry the affected player and a creation timestamp so
 * listeners can act without needing to re-query for context they already have.
 */
public interface DomainEvent {
    Long playerId();
    Instant occurredAt();
}
