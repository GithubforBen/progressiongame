package com.financegame.repository;

import com.financegame.entity.PlayerInvestmentLevel;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Legacy repository — no longer used; player_investment_levels table was dropped in V10.
 */
@Repository
public class PlayerInvestmentLevelRepository {

    public Optional<PlayerInvestmentLevel> findByPlayerId(Long playerId) {
        return Optional.empty();
    }

    public PlayerInvestmentLevel save(PlayerInvestmentLevel entity) {
        return entity;
    }
}
