package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_social_relationships")
@IdClass(PlayerSocialRelationshipId.class)
public class PlayerSocialRelationship {

    @Id
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Id
    @Column(name = "person_id", nullable = false)
    private String personId;

    @Column(name = "score", nullable = false)
    private int score = 0;

    @Column(name = "unlocked_at_turn")
    private Integer unlockedAtTurn;

    @Column(name = "locked_actions_until_turn", nullable = false)
    private int lockedActionsUntilTurn = 0;

    @Column(name = "monthly_time_spent_count", nullable = false)
    private int monthlyTimeSpentCount = 0;

    @Column(name = "monthly_insult_done", nullable = false)
    private boolean monthlyInsultDone = false;

    @Column(name = "monthly_rob_attempted", nullable = false)
    private boolean monthlyRobAttempted = false;

    @Column(name = "had_conflict", nullable = false)
    private boolean hadConflict = false;

    public PlayerSocialRelationship() {}

    public PlayerSocialRelationship(Long playerId, String personId, int unlockedAtTurn) {
        this.playerId = playerId;
        this.personId = personId;
        this.unlockedAtTurn = unlockedAtTurn;
    }

    public Long getPlayerId() { return playerId; }
    public String getPersonId() { return personId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public Integer getUnlockedAtTurn() { return unlockedAtTurn; }
    public void setUnlockedAtTurn(Integer unlockedAtTurn) { this.unlockedAtTurn = unlockedAtTurn; }

    public int getLockedActionsUntilTurn() { return lockedActionsUntilTurn; }
    public void setLockedActionsUntilTurn(int lockedActionsUntilTurn) { this.lockedActionsUntilTurn = lockedActionsUntilTurn; }

    public int getMonthlyTimeSpentCount() { return monthlyTimeSpentCount; }
    public void setMonthlyTimeSpentCount(int monthlyTimeSpentCount) { this.monthlyTimeSpentCount = monthlyTimeSpentCount; }

    public boolean isMonthlyInsultDone() { return monthlyInsultDone; }
    public void setMonthlyInsultDone(boolean monthlyInsultDone) { this.monthlyInsultDone = monthlyInsultDone; }

    public boolean isMonthlyRobAttempted() { return monthlyRobAttempted; }
    public void setMonthlyRobAttempted(boolean monthlyRobAttempted) { this.monthlyRobAttempted = monthlyRobAttempted; }

    public boolean isHadConflict() { return hadConflict; }
    public void setHadConflict(boolean hadConflict) { this.hadConflict = hadConflict; }
}
