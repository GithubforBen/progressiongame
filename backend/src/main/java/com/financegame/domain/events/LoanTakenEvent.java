package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by LoanService when a new loan is successfully created.
 * Allows listeners to log the event outside of the turn cycle.
 */
public record LoanTakenEvent(
    Long playerId,
    Long loanId,
    BigDecimal amount,
    String label,
    Instant occurredAt
) implements DomainEvent {
    public LoanTakenEvent(Long playerId, Long loanId, BigDecimal amount, String label) {
        this(playerId, loanId, amount, label, Instant.now());
    }
}
