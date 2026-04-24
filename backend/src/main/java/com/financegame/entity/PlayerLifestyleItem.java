package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_lifestyle_items")
@IdClass(PlayerLifestyleItemId.class)
public class PlayerLifestyleItem {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Id
    @Column(name = "item_id", length = 50)
    private String itemId;

    @Column(name = "acquired_at_turn", nullable = false)
    private int acquiredAtTurn;

    public PlayerLifestyleItem() {}

    public PlayerLifestyleItem(Long playerId, String itemId, int acquiredAtTurn) {
        this.playerId = playerId;
        this.itemId = itemId;
        this.acquiredAtTurn = acquiredAtTurn;
    }

    public Long getPlayerId() { return playerId; }
    public String getItemId() { return itemId; }
    public int getAcquiredAtTurn() { return acquiredAtTurn; }
    public void setPlayerId(Long id) { this.playerId = id; }
    public void setItemId(String id) { this.itemId = id; }
    public void setAcquiredAtTurn(int t) { this.acquiredAtTurn = t; }
}
