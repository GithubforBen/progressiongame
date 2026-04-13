package com.financegame.dto;

import java.math.BigDecimal;
import java.util.List;

public record PokerResultDto(
    List<String> playerCards,
    List<String> aiCards,
    String playerHandName,
    String aiHandName,
    String result,
    BigDecimal betAmount,
    BigDecimal payout,
    BigDecimal netChange
) {}
