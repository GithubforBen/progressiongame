package com.financegame.dto;

import java.math.BigDecimal;

public record LeaderboardEntryDto(
    int rank,
    Long playerId,
    String username,
    BigDecimal netWorth,       // gross: cash + investments + real estate
    BigDecimal totalDebt,      // sum of active loan amounts_remaining
    BigDecimal adjustedNetWorth, // netWorth - totalDebt (used for ranking)
    BigDecimal monthlyIncome,
    int completedCollections,
    int currentTurn,
    boolean isMe
) {}
