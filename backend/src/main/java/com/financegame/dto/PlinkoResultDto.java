package com.financegame.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlinkoResultDto(
    List<BallResult> balls,
    int ballCount,
    BigDecimal ballValue,
    BigDecimal betAmount,
    BigDecimal totalPayout,
    BigDecimal netChange
) {
    public record BallResult(
        List<Boolean> path,
        int slot,
        BigDecimal multiplier,
        BigDecimal payout
    ) {}
}
