package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "player_collectibles")
@IdClass(PlayerCollectibleId.class)
public class PlayerCollectible {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Id
    @Column(name = "collectible_id")
    private Long collectibleId;

    @Column(name = "acquired_at_turn", nullable = false)
    private int acquiredAtTurn;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    public PlayerCollectible() {}

    public PlayerCollectible(Long playerId, Long collectibleId, int acquiredAtTurn, BigDecimal purchasePrice) {
        this.playerId = playerId;
        this.collectibleId = collectibleId;
        this.acquiredAtTurn = acquiredAtTurn;
        this.purchasePrice = purchasePrice;
    }

    public Long getPlayerId() { return playerId; }
    public Long getCollectibleId() { return collectibleId; }
    public int getAcquiredAtTurn() { return acquiredAtTurn; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
}
