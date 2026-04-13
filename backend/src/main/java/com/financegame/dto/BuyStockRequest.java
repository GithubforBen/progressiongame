package com.financegame.dto;

import java.math.BigDecimal;

public record BuyStockRequest(
    String ticker,
    BigDecimal quantity
) {}
