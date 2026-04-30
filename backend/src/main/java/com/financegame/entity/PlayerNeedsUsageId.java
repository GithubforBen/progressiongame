package com.financegame.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class PlayerNeedsUsageId {

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "item_id", length = 50)
    private String itemId;

    public PlayerNeedsUsageId() {}

    public PlayerNeedsUsageId(Long playerId, String itemId) {
        this.playerId = playerId;
        this.itemId = itemId;
    }

    public Long getPlayerId() { return playerId; }
    public String getItemId() { return itemId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerNeedsUsageId that)) return false;
        return Objects.equals(playerId, that.playerId) && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() { return Objects.hash(playerId, itemId); }
}
