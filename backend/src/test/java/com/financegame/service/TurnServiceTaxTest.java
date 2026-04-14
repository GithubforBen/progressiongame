package com.financegame.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the progressive income-tax calculation in TurnService.
 * Brackets: 0–1000 € → 0%, 1001–3000 € → 20%, 3001–6000 € → 32%, >6000 € → 42%
 */
class TurnServiceTaxTest {

    @Test
    void belowOrEqualFirstBracket_noTax() {
        assertThat(TurnService.calculateTax(BigDecimal.valueOf(0))).isEqualByComparingTo("0.00");
        assertThat(TurnService.calculateTax(BigDecimal.valueOf(1000))).isEqualByComparingTo("0.00");
    }

    @Test
    void withinSecondBracket_20Percent() {
        // Income 1500 → taxable 500 × 20% = 100
        assertThat(TurnService.calculateTax(BigDecimal.valueOf(1500))).isEqualByComparingTo("100.00");
    }

    @Test
    void atTopOfSecondBracket_20Percent() {
        // Income 3000 → taxable 2000 × 20% = 400
        assertThat(TurnService.calculateTax(BigDecimal.valueOf(3000))).isEqualByComparingTo("400.00");
    }

    @Test
    void withinThirdBracket_blended() {
        // Income 4000 → 2000×20% + 1000×32% = 400 + 320 = 720
        assertThat(TurnService.calculateTax(BigDecimal.valueOf(4000))).isEqualByComparingTo("720.00");
    }

    @Test
    void withinFourthBracket_blended() {
        // Income 8000 → 2000×20% + 3000×32% + 2000×42% = 400 + 960 + 840 = 2200
        assertThat(TurnService.calculateTax(BigDecimal.valueOf(8000))).isEqualByComparingTo("2200.00");
    }

    @Test
    void exactlyAtThirdBracketTop() {
        // Income 6000 → 2000×20% + 3000×32% = 400 + 960 = 1360
        assertThat(TurnService.calculateTax(BigDecimal.valueOf(6000))).isEqualByComparingTo("1360.00");
    }
}
