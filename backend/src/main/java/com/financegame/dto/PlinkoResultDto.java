package com.financegame.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlinkoResultDto(
    List<Boolean> path,
    int slot,
    BigDecimal multiplier,
    BigDecimal betAmount,
    BigDecimal payout,
    BigDecimal netChange
) {}
