package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by TurnService when the tax-evasion detection check succeeds.
 * The player has been caught and taxEvasionCaughtPending is set to true.
 * The actual jail/exile sentence is determined later when the player pays bail
 * via TaxEvasionService.payBail().
 */
public record TaxEvasionCaughtEvent(
    Long playerId,
    BigDecimal evadedAmount,
    Instant occurredAt
) implements DomainEvent {
    public TaxEvasionCaughtEvent(Long playerId, BigDecimal evadedAmount) {
        this(playerId, evadedAmount, Instant.now());
    }
}
