package com.financegame.dto;

import java.math.BigDecimal;

public record SellStockResponse(
    String ticker,
    BigDecimal proceeds,
    BigDecimal costBasis,
    BigDecimal grossProfit,
    BigDecimal taxPaid,
    BigDecimal netProceeds
) {}
