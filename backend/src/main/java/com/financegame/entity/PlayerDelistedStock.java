package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_delisted_stocks")
public class PlayerDelistedStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "delisted_at_turn", nullable = false)
    private int delistedAtTurn;

    public PlayerDelistedStock() {}

    public PlayerDelistedStock(Long playerId, Long stockId, int delistedAtTurn) {
        this.playerId = playerId;
        this.stockId = stockId;
        this.delistedAtTurn = delistedAtTurn;
    }

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getStockId() { return stockId; }
    public int getDelistedAtTurn() { return delistedAtTurn; }
}
