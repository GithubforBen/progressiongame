package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_needs_usage")
public class PlayerNeedsUsage {

    @EmbeddedId
    private PlayerNeedsUsageId id;

    @Column(name = "last_used_turn", nullable = false)
    private int lastUsedTurn;

    public PlayerNeedsUsage() {}

    public PlayerNeedsUsage(Long playerId, String itemId, int lastUsedTurn) {
        this.id = new PlayerNeedsUsageId(playerId, itemId);
        this.lastUsedTurn = lastUsedTurn;
    }

    public PlayerNeedsUsageId getId() { return id; }
    public void setId(PlayerNeedsUsageId id) { this.id = id; }
    public int getLastUsedTurn() { return lastUsedTurn; }
    public void setLastUsedTurn(int lastUsedTurn) { this.lastUsedTurn = lastUsedTurn; }
}
