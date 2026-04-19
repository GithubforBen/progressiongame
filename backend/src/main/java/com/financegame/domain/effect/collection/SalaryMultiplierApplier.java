package com.financegame.domain.effect.collection;

import com.financegame.dto.TurnResultDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class SalaryMultiplierApplier implements CollectionBonusApplier {

    @Override
    public String getBonusType() { return "SALARY_MULTIPLIER"; }

    @Override
    public BigDecimal modifyIncome(BigDecimal income, BigDecimal bonusValue,
                                   List<TurnResultDto.LineItem> breakdown) {
        return income.multiply(BigDecimal.ONE.add(bonusValue)).setScale(2, RoundingMode.HALF_UP);
    }
}
