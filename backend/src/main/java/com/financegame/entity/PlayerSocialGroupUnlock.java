package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_social_group_unlocks")
@IdClass(PlayerSocialGroupUnlockId.class)
public class PlayerSocialGroupUnlock {

    @Id
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Id
    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "unlocked_at_turn", nullable = false)
    private int unlockedAtTurn;

    public PlayerSocialGroupUnlock() {}

    public PlayerSocialGroupUnlock(Long playerId, String groupId, int unlockedAtTurn) {
        this.playerId = playerId;
        this.groupId = groupId;
        this.unlockedAtTurn = unlockedAtTurn;
    }

    public Long getPlayerId() { return playerId; }
    public String getGroupId() { return groupId; }
    public int getUnlockedAtTurn() { return unlockedAtTurn; }
}
