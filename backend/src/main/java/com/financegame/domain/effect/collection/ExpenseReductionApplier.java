package com.financegame.domain.effect.collection;

import com.financegame.dto.TurnResultDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ExpenseReductionApplier implements CollectionBonusApplier {

    @Override
    public String getBonusType() { return "EXPENSE_REDUCTION"; }

    @Override
    public BigDecimal modifyExpenses(BigDecimal expenses, BigDecimal bonusValue) {
        return expenses.multiply(BigDecimal.ONE.subtract(bonusValue)).setScale(2, RoundingMode.HALF_UP);
    }
}
