package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

import java.math.BigDecimal;

public class MinNetWorthCondition implements Condition {

    private final BigDecimal minAmount;

    public MinNetWorthCondition(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    @Override
    public boolean isMet(GameContext context) {
        return context.character().getNetWorth().compareTo(minAmount) >= 0;
    }

    @Override
    public String describe() {
        return "Mindestvermögen: " + minAmount.toPlainString() + "€";
    }
}
