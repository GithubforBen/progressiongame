package com.financegame.dto;

import java.math.BigDecimal;

public record AdminCharacterEditRequest(
    BigDecimal cash,
    Integer stress,
    Integer happiness,
    Integer energy,
    Integer hunger,
    Integer schufaScore,
    Integer jailMonthsRemaining,
    Integer exileMonthsRemaining,
    Boolean burnoutActive
) {}
