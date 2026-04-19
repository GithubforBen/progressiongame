package com.financegame.domain.events;

import java.time.Instant;

/**
 * Fired by TurnService when a loan's turnsRemaining reaches 0 and status transitions to PAID_OFF.
 * LoanEventListener logs this milestone. SCHUFA update is handled inline in TurnService
 * because it affects the current turn's net-worth calculation.
 */
public record LoanPaidOffEvent(
    Long playerId,
    Long loanId,
    String label,
    Instant occurredAt
) implements DomainEvent {
    public LoanPaidOffEvent(Long playerId, Long loanId, String label) {
        this(playerId, loanId, label, Instant.now());
    }
}
