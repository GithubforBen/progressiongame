package com.financegame.dto;

import java.math.BigDecimal;

public record CollectionDto(
    String name,
    String displayName,
    String bonusType,
    BigDecimal bonusValue,
    int itemCount,
    int ownedCount,
    boolean completed,
    String requiredCert,
    boolean locked
) {}
