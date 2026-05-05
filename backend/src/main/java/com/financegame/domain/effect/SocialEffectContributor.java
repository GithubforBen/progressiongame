package com.financegame.domain.effect;

import com.financegame.service.SocialService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contributes effects from active social relationship boosts.
 * Values are score-weighted per person (score/100 × boost.value),
 * summed across all persons with the same boost type by SocialService.
 */
@Component
public class SocialEffectContributor implements EffectContributor {

    private static final Map<String, EffectType> TYPE_MAP = Map.ofEntries(
        Map.entry("SALARY_MULTIPLIER_BONUS",      EffectType.SALARY_MULTIPLIER),
        Map.entry("MONTHLY_INCOME_BONUS",          EffectType.MONTHLY_INCOME_BONUS),
        Map.entry("EXPENSE_REDUCTION",             EffectType.EXPENSE_REDUCTION),
        Map.entry("HAPPINESS_PER_TURN",            EffectType.HAPPINESS_PER_TURN),
        Map.entry("STRESS_REDUCTION_PER_TURN",     EffectType.STRESS_REDUCTION_PER_TURN),
        Map.entry("ENERGY_BONUS_PER_TURN",         EffectType.ENERGY_BONUS_PER_TURN),
        Map.entry("SCHUFA_BONUS_MONTHLY",          EffectType.SCHUFA_BONUS_MONTHLY),
        Map.entry("LOAN_INTEREST_REDUCTION",       EffectType.LOAN_INTEREST_REDUCTION),
        Map.entry("PROPERTY_PRICE_DISCOUNT",       EffectType.PROPERTY_PRICE_DISCOUNT),
        Map.entry("TRAVEL_COST_REDUCTION",         EffectType.TRAVEL_COST_REDUCTION),
        Map.entry("TRAVEL_DURATION_REDUCTION",     EffectType.TRAVEL_DURATION_REDUCTION),
        Map.entry("COLLECTIBLE_PRICE_DISCOUNT",    EffectType.COLLECTIBLE_PRICE_DISCOUNT),
        Map.entry("TAX_DETECTION_REDUCTION",       EffectType.TAX_DETECTION_REDUCTION),
        Map.entry("HUNGER_DECAY_REDUCTION",        EffectType.HUNGER_DECAY_REDUCTION),
        Map.entry("JOB_ACCEPTANCE_BOOST",          EffectType.JOB_ACCEPTANCE_BOOST),
        Map.entry("SALARY_BONUS",                  EffectType.SALARY_BONUS),
        Map.entry("STOCK_VOLATILITY_REDUCTION",    EffectType.STOCK_VOLATILITY_REDUCTION),
        Map.entry("GAMBLING_LUCK_BOOST",           EffectType.GAMBLING_LUCK_BOOST),
        Map.entry("COLLECTIBLE_DROP_RATE_BOOST",   EffectType.COLLECTIBLE_DROP_RATE_BOOST),
        Map.entry("ROB_SUCCESS_BOOST",             EffectType.ROB_SUCCESS_BOOST),
        Map.entry("ROB_LOOT_MULTIPLIER",           EffectType.ROB_LOOT_MULTIPLIER)
    );

    private final SocialService socialService;

    public SocialEffectContributor(SocialService socialService) {
        this.socialService = socialService;
    }

    @Override
    public List<EffectContribution> getContributions(Long playerId) {
        List<EffectContribution> result = new ArrayList<>();
        for (SocialService.ActiveBoostDto boost : socialService.getActiveBoosts(playerId)) {
            EffectType type = TYPE_MAP.get(boost.type());
            if (type == null) continue;
            result.add(new EffectContribution(type, boost.totalValue(), "Beziehung", boost.type()));
        }
        return result;
    }
}
