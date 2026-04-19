package com.financegame.dto;

import java.util.List;

public record SchufaBreakdownDto(
    int score,
    List<SchufaFactor> factors
) {
    public record SchufaFactor(String label, int impact, String detail) {}
}
