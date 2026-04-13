package com.financegame.dto;

import java.math.BigDecimal;

public record SlotResultDto(
    String[] reels,
    String outcome,
    BigDecimal betAmount,
    BigDecimal payout,
    BigDecimal netChange
) {}
