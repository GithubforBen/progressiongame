package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by InvestmentService when a player sells an investment position.
 * profitLoss is positive for a gain, negative for a loss.
 */
public record StockSoldEvent(
    Long playerId,
    String ticker,
    BigDecimal proceeds,
    BigDecimal profitLoss,
    Instant occurredAt
) implements DomainEvent {
    public StockSoldEvent(Long playerId, String ticker, BigDecimal proceeds, BigDecimal profitLoss) {
        this(playerId, ticker, proceeds, profitLoss, Instant.now());
    }
}
