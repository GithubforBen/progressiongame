package com.financegame.entity;

import java.io.Serializable;
import java.util.Objects;

public class PlayerCollectibleId implements Serializable {

    private Long playerId;
    private Long collectibleId;

    public PlayerCollectibleId() {}

    public PlayerCollectibleId(Long playerId, Long collectibleId) {
        this.playerId = playerId;
        this.collectibleId = collectibleId;
    }

    public Long getPlayerId() { return playerId; }
    public Long getCollectibleId() { return collectibleId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerCollectibleId that)) return false;
        return Objects.equals(playerId, that.playerId) && Objects.equals(collectibleId, that.collectibleId);
    }

    @Override
    public int hashCode() { return Objects.hash(playerId, collectibleId); }
}
