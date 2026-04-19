package com.financegame.service;

import com.financegame.config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the progressive income-tax calculation in TaxService.
 * Brackets: 0–1000 € → 0%, 1001–3000 € → 20%, 3001–6000 € → 32%, >6000 € → 42%
 */
class TurnServiceTaxTest {

    TaxService taxService;

    @BeforeEach
    void setUp() {
        GameConfig config = new GameConfig();
        GameConfig.TaxConfig taxConfig = new GameConfig.TaxConfig();
        taxConfig.setBrackets(List.of(
            bracket(1000, 0.0, "0 – 1.000 €"),
            bracket(3000, 0.20, "1.001 – 3.000 €"),
            bracket(6000, 0.32, "3.001 – 6.000 €"),
            bracket(999_999_999, 0.42, "Über 6.000 €")
        ));
        config.setTax(taxConfig);
        taxService = new TaxService(config);
    }

    @Test
    void belowOrEqualFirstBracket_noTax() {
        assertThat(taxService.calculateTax(BigDecimal.valueOf(0))).isEqualByComparingTo("0.00");
        assertThat(taxService.calculateTax(BigDecimal.valueOf(1000))).isEqualByComparingTo("0.00");
    }

    @Test
    void withinSecondBracket_20Percent() {
        // Income 1500 → taxable 500 × 20% = 100
        assertThat(taxService.calculateTax(BigDecimal.valueOf(1500))).isEqualByComparingTo("100.00");
    }

    @Test
    void atTopOfSecondBracket_20Percent() {
        // Income 3000 → taxable 2000 × 20% = 400
        assertThat(taxService.calculateTax(BigDecimal.valueOf(3000))).isEqualByComparingTo("400.00");
    }

    @Test
    void withinThirdBracket_blended() {
        // Income 4000 → 2000×20% + 1000×32% = 400 + 320 = 720
        assertThat(taxService.calculateTax(BigDecimal.valueOf(4000))).isEqualByComparingTo("720.00");
    }

    @Test
    void withinFourthBracket_blended() {
        // Income 8000 → 2000×20% + 3000×32% + 2000×42% = 400 + 960 + 840 = 2200
        assertThat(taxService.calculateTax(BigDecimal.valueOf(8000))).isEqualByComparingTo("2200.00");
    }

    @Test
    void exactlyAtThirdBracketTop() {
        // Income 6000 → 2000×20% + 3000×32% = 400 + 960 = 1360
        assertThat(taxService.calculateTax(BigDecimal.valueOf(6000))).isEqualByComparingTo("1360.00");
    }

    // ── bracket label / percent ────────────────────────────────────────────────

    @Test
    void bracketLabel_belowFirst() {
        assertThat(taxService.determineBracketLabel(BigDecimal.valueOf(500))).isEqualTo("0 – 1.000 €");
    }

    @Test
    void bracketLabel_above6000() {
        assertThat(taxService.determineBracketLabel(BigDecimal.valueOf(9000))).isEqualTo("Über 6.000 €");
    }

    @Test
    void bracketPercent_secondBracket() {
        assertThat(taxService.determineBracketPercent(BigDecimal.valueOf(2000))).isEqualTo(20);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static GameConfig.TaxConfig.TaxBracket bracket(double upTo, double rate, String label) {
        GameConfig.TaxConfig.TaxBracket b = new GameConfig.TaxConfig.TaxBracket();
        b.setUpTo(upTo);
        b.setRate(rate);
        b.setLabel(label);
        return b;
    }
}
