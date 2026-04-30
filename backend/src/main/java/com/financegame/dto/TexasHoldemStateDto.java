package com.financegame.dto;

import java.math.BigDecimal;
import java.util.List;

public record TexasHoldemStateDto(
    Long sessionId,
    List<String> playerCards,
    List<String> communityCards,
    BigDecimal pot,
    BigDecimal playerStake,
    BigDecimal initialBet,
    BigDecimal toCall,
    BigDecimal raiseCost,
    List<BotInfo> bots,
    String street,
    String status,
    boolean awaitingPlayerResponse,
    List<ActionEntry> actionLog,
    String playerCurrentHandName,
    List<DrawInfo> playerDraws,
    BigDecimal payout,
    BigDecimal netChange
) {
    public record BotInfo(
        int index,
        boolean folded,
        List<String> cards,
        String handName,
        boolean winner,
        String personality,
        String riskProfile
    ) {}

    public record ActionEntry(
        String actor,
        String actionType,
        BigDecimal amount
    ) {}

    public record DrawInfo(
        String type,
        int outs,
        double probability,
        String description
    ) {}
}
