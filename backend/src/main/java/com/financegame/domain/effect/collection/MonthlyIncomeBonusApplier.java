package com.financegame.domain.effect.collection;

import com.financegame.dto.TurnResultDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MonthlyIncomeBonusApplier implements CollectionBonusApplier {

    @Override
    public String getBonusType() { return "MONTHLY_INCOME_BONUS"; }

    @Override
    public BigDecimal modifyIncome(BigDecimal income, BigDecimal bonusValue,
                                   List<TurnResultDto.LineItem> breakdown) {
        breakdown.add(new TurnResultDto.LineItem("Sammlungs-Bonus", bonusValue));
        return income.add(bonusValue);
    }
}
