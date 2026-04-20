package com.financegame.entity;

import java.io.Serializable;
import java.util.Objects;

public class PlayerSocialGroupUnlockId implements Serializable {

    private Long playerId;
    private String groupId;

    public PlayerSocialGroupUnlockId() {}

    public PlayerSocialGroupUnlockId(Long playerId, String groupId) {
        this.playerId = playerId;
        this.groupId = groupId;
    }

    public Long getPlayerId() { return playerId; }
    public String getGroupId() { return groupId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerSocialGroupUnlockId that)) return false;
        return Objects.equals(playerId, that.playerId) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() { return Objects.hash(playerId, groupId); }
}
