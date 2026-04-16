package com.financegame.entity;

/**
 * Legacy class — player_investment_levels table was dropped in V10.
 * Investment unlocking is now cert-based via EducationProgress.
 */
public class PlayerInvestmentLevel {
    private Long playerId;

    public PlayerInvestmentLevel() {}
    public PlayerInvestmentLevel(Long playerId) { this.playerId = playerId; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
}
