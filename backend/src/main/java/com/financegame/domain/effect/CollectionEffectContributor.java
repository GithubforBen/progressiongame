package com.financegame.domain.effect;

import com.financegame.service.CollectionService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contributes effects from completed collections (bonus_type / bonus_value from the DB).
 * A collection bonus is only active when the player has completed the full collection.
 */
@Component
public class CollectionEffectContributor implements EffectContributor {

    private static final Map<String, EffectType> TYPE_MAP = Map.of(
        "SALARY_MULTIPLIER",    EffectType.SALARY_MULTIPLIER,
        "MONTHLY_INCOME_BONUS", EffectType.MONTHLY_INCOME_BONUS,
        "EXPENSE_REDUCTION",    EffectType.EXPENSE_REDUCTION,
        "HAPPINESS_BONUS",      EffectType.HAPPINESS_PER_TURN,
        "SCHUFA_BONUS",         EffectType.SCHUFA_BONUS_MONTHLY
    );

    private final CollectionService collectionService;

    public CollectionEffectContributor(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @Override
    public List<EffectContribution> getContributions(Long playerId) {
        List<EffectContribution> result = new ArrayList<>();
        for (CollectionService.ActiveBonus bonus : collectionService.getActiveCollectionBonuses(playerId)) {
            EffectType type = TYPE_MAP.get(bonus.bonusType());
            if (type == null) continue;
            result.add(new EffectContribution(
                type,
                bonus.bonusValue().doubleValue(),
                "Sammlung",
                bonus.bonusType()
            ));
        }
        return result;
    }
}
