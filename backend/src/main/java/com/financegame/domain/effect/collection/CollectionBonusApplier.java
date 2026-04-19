package com.financegame.domain.effect.collection;

import com.financegame.dto.TurnResultDto;
import com.financegame.entity.GameCharacter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strategy for applying one type of collection completion bonus during turn processing.
 * Implement as a Spring @Component for each bonus type; TurnService discovers them all
 * via dependency injection and builds a Map<bonusType, applier>.
 *
 * Adding a new bonus type = add one new @Component. No changes to TurnService.
 */
public interface CollectionBonusApplier {

    /** The bonusType string stored on the Collection entity (e.g. "SALARY_MULTIPLIER"). */
    String getBonusType();

    /**
     * Modify gross income for this bonus. Called during the income phase.
     * Default: return income unchanged.
     */
    default BigDecimal modifyIncome(BigDecimal income, BigDecimal bonusValue,
                                    List<TurnResultDto.LineItem> breakdown) {
        return income;
    }

    /**
     * Modify total expenses for this bonus. Called during the expense phase.
     * Default: return expenses unchanged.
     */
    default BigDecimal modifyExpenses(BigDecimal expenses, BigDecimal bonusValue) {
        return expenses;
    }

    /**
     * Apply stat changes (happiness, SCHUFA, etc.) directly to the character.
     * Default: no-op.
     */
    default void applyStats(GameCharacter character, BigDecimal bonusValue) {}
}
