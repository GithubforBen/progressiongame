package com.financegame.entity;

import java.io.Serializable;
import java.util.Objects;

public class PlayerLifestyleItemId implements Serializable {
    private Long playerId;
    private String itemId;

    public PlayerLifestyleItemId() {}
    public PlayerLifestyleItemId(Long playerId, String itemId) {
        this.playerId = playerId;
        this.itemId = itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerLifestyleItemId)) return false;
        PlayerLifestyleItemId that = (PlayerLifestyleItemId) o;
        return Objects.equals(playerId, that.playerId) && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() { return Objects.hash(playerId, itemId); }
}
