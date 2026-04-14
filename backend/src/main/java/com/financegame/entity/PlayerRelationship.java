package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_relationships")
public class PlayerRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "npc_id", nullable = false)
    private Long npcId;

    @Column(name = "level", nullable = false)
    private int level = 0;

    @Column(name = "months_known", nullable = false)
    private int monthsKnown = 0;

    @Column(name = "last_interacted_turn", nullable = false)
    private int lastInteractedTurn = -1;

    public PlayerRelationship() {}

    public PlayerRelationship(Long playerId, Long npcId) {
        this.playerId = playerId;
        this.npcId = npcId;
    }

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getNpcId() { return npcId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getMonthsKnown() { return monthsKnown; }
    public void setMonthsKnown(int monthsKnown) { this.monthsKnown = monthsKnown; }

    public int getLastInteractedTurn() { return lastInteractedTurn; }
    public void setLastInteractedTurn(int turn) { this.lastInteractedTurn = turn; }
}
