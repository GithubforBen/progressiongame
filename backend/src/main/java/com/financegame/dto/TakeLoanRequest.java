package com.financegame.dto;

import java.math.BigDecimal;

public record TakeLoanRequest(String label, BigDecimal amount, int termMonths) {}
