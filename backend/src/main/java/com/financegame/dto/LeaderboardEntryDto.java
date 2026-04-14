package com.financegame.dto;

import java.math.BigDecimal;

public record LeaderboardEntryDto(
    int rank,
    Long playerId,
    String username,
    BigDecimal netWorth,
    int currentTurn,
    boolean isMe
) {}
