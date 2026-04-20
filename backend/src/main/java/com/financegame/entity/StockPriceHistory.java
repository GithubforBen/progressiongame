package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_price_history")
public class StockPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "turn", nullable = false)
    private int turn;

    public StockPriceHistory() {}

    public StockPriceHistory(Long stockId, Long playerId, BigDecimal price, int turn) {
        this.stockId = stockId;
        this.playerId = playerId;
        this.price = price;
        this.turn = turn;
    }

    public Long getId() { return id; }
    public Long getStockId() { return stockId; }
    public Long getPlayerId() { return playerId; }
    public BigDecimal getPrice() { return price; }
    public int getTurn() { return turn; }
}
