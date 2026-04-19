package com.financegame.domain.effect;

import com.financegame.domain.effect.collection.ExpenseReductionApplier;
import com.financegame.domain.effect.collection.HappinessBonusApplier;
import com.financegame.domain.effect.collection.MonthlyIncomeBonusApplier;
import com.financegame.domain.effect.collection.SalaryMultiplierApplier;
import com.financegame.domain.effect.collection.SchufaBonusApplier;
import com.financegame.dto.TurnResultDto;
import com.financegame.entity.GameCharacter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionBonusApplierTest {

    // ── SalaryMultiplierApplier ───────────────────────────────────────────────

    @Test
    void salaryMultiplier_booststIncome() {
        SalaryMultiplierApplier applier = new SalaryMultiplierApplier();
        BigDecimal result = applier.modifyIncome(BigDecimal.valueOf(1000), BigDecimal.valueOf(0.10), List.of());
        assertThat(result).isEqualByComparingTo("1100.00");
    }

    @Test
    void salaryMultiplier_bonusType() {
        assertThat(new SalaryMultiplierApplier().getBonusType()).isEqualTo("SALARY_MULTIPLIER");
    }

    // ── MonthlyIncomeBonusApplier ─────────────────────────────────────────────

    @Test
    void monthlyIncomeBonus_addsToIncomeAndBreakdown() {
        MonthlyIncomeBonusApplier applier = new MonthlyIncomeBonusApplier();
        List<TurnResultDto.LineItem> breakdown = new ArrayList<>();
        BigDecimal result = applier.modifyIncome(BigDecimal.valueOf(2000), BigDecimal.valueOf(150), breakdown);
        assertThat(result).isEqualByComparingTo("2150");
        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).amount()).isEqualByComparingTo("150");
    }

    // ── ExpenseReductionApplier ───────────────────────────────────────────────

    @Test
    void expenseReduction_reducesExpenses() {
        ExpenseReductionApplier applier = new ExpenseReductionApplier();
        BigDecimal result = applier.modifyExpenses(BigDecimal.valueOf(1000), BigDecimal.valueOf(0.10));
        assertThat(result).isEqualByComparingTo("900.00");
    }

    @Test
    void expenseReduction_bonusType() {
        assertThat(new ExpenseReductionApplier().getBonusType()).isEqualTo("EXPENSE_REDUCTION");
    }

    // ── HappinessBonusApplier ─────────────────────────────────────────────────

    @Test
    void happinessBonus_increasesHappiness() {
        HappinessBonusApplier applier = new HappinessBonusApplier();
        GameCharacter character = new GameCharacter();
        character.setHappiness(60);
        applier.applyStats(character, BigDecimal.valueOf(10));
        assertThat(character.getHappiness()).isEqualTo(70);
    }

    @Test
    void happinessBonus_clampsAt100() {
        HappinessBonusApplier applier = new HappinessBonusApplier();
        GameCharacter character = new GameCharacter();
        character.setHappiness(95);
        applier.applyStats(character, BigDecimal.valueOf(20));
        assertThat(character.getHappiness()).isEqualTo(100);
    }

    // ── SchufaBonusApplier ────────────────────────────────────────────────────

    @Test
    void schufaBonus_increasesScore() {
        SchufaBonusApplier applier = new SchufaBonusApplier();
        GameCharacter character = new GameCharacter();
        character.setSchufaScore(500);
        applier.applyStats(character, BigDecimal.valueOf(50));
        assertThat(character.getSchufaScore()).isEqualTo(550);
    }

    @Test
    void schufaBonus_clampsAt1000() {
        SchufaBonusApplier applier = new SchufaBonusApplier();
        GameCharacter character = new GameCharacter();
        character.setSchufaScore(990);
        applier.applyStats(character, BigDecimal.valueOf(50));
        assertThat(character.getSchufaScore()).isEqualTo(1000);
    }
}
