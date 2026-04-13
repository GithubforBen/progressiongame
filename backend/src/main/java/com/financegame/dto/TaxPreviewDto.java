package com.financegame.dto;

import java.math.BigDecimal;

public record TaxPreviewDto(
    BigDecimal grossIncome,
    BigDecimal taxAmount,
    BigDecimal netIncome,
    String bracket,
    int bracketPercent
) {}
