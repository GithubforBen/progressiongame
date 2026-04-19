package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by InvestmentService when a player buys shares of a stock.
 * Allows listeners to log the purchase or detect unusual trading patterns.
 */
public record StockPurchasedEvent(
    Long playerId,
    String ticker,
    BigDecimal quantity,
    BigDecimal totalCost,
    Instant occurredAt
) implements DomainEvent {
    public StockPurchasedEvent(Long playerId, String ticker, BigDecimal quantity, BigDecimal totalCost) {
        this(playerId, ticker, quantity, totalCost, Instant.now());
    }
}
