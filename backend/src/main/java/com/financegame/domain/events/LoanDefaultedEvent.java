package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by TurnService when a player cannot meet a monthly loan payment and the loan
 * transitions to DEFAULTED. amountRemaining captures the outstanding balance at the time
 * of default. SCHUFA penalty is applied inline in TurnService.
 */
public record LoanDefaultedEvent(
    Long playerId,
    Long loanId,
    String label,
    BigDecimal amountRemaining,
    Instant occurredAt
) implements DomainEvent {
    public LoanDefaultedEvent(Long playerId, Long loanId, String label, BigDecimal amountRemaining) {
        this(playerId, loanId, label, amountRemaining, Instant.now());
    }
}
