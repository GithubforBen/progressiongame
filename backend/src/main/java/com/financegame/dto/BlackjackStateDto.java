package com.financegame.dto;

import java.math.BigDecimal;
import java.util.List;

public record BlackjackStateDto(
    Long sessionId,
    List<String> playerCards,
    List<String> dealerCards,
    int playerTotal,
    int dealerVisible,
    String status,
    BigDecimal betAmount,
    BigDecimal payout,
    BigDecimal netChange
) {}
