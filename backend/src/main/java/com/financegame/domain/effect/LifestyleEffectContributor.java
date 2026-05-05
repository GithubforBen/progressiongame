package com.financegame.domain.effect;

import com.financegame.entity.LifestyleItemCatalog;
import com.financegame.service.LifestyleService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Contributes effects from owned lifestyle items.
 * stressReductionMonth → STRESS_REDUCTION_PER_TURN
 * taxEvasionBoost      → TAX_DETECTION_REDUCTION (15% reduction per item with flag)
 */
@Component
public class LifestyleEffectContributor implements EffectContributor {

    private final LifestyleService lifestyleService;

    public LifestyleEffectContributor(LifestyleService lifestyleService) {
        this.lifestyleService = lifestyleService;
    }

    @Override
    public List<EffectContribution> getContributions(Long playerId) {
        List<EffectContribution> result = new ArrayList<>();
        for (LifestyleItemCatalog item : lifestyleService.getOwnedCatalogItems(playerId)) {
            if (item.getStressReductionMonth() > 0) {
                result.add(new EffectContribution(
                    EffectType.STRESS_REDUCTION_PER_TURN,
                    item.getStressReductionMonth(),
                    "Lifestyle",
                    item.getName()
                ));
            }
            if (item.isTaxEvasionBoost()) {
                result.add(new EffectContribution(
                    EffectType.TAX_DETECTION_REDUCTION,
                    0.15,
                    "Lifestyle",
                    item.getName()
                ));
            }
        }
        return result;
    }
}
