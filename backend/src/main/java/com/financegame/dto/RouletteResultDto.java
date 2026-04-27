package com.financegame.dto;

import java.math.BigDecimal;
import java.util.List;

public record RouletteResultDto(
    int winningNumber,
    String winningColor,
    BigDecimal totalBet,
    BigDecimal totalPayout,
    BigDecimal netChange,
    List<BetResult> betResults
) {
    public record BetResult(
        String type,
        List<Integer> numbers,
        BigDecimal amount,
        boolean won,
        BigDecimal payout
    ) {}
}
