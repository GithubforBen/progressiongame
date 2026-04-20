package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

public record RobAttemptedEvent(
    Long playerId,
    String personId,
    boolean success,
    boolean caught,
    BigDecimal lootAmount,
    Instant occurredAt
) implements DomainEvent {
    public RobAttemptedEvent(Long playerId, String personId, boolean success, boolean caught, BigDecimal lootAmount) {
        this(playerId, personId, success, caught, lootAmount, Instant.now());
    }
}
