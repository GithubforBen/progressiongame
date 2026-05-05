package com.financegame.service;

import com.financegame.domain.effect.EffectContribution;
import com.financegame.domain.effect.EffectContributor;
import com.financegame.domain.effect.PlayerEffects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Central aggregator of all player effects (bonuses, discounts, stat-ticks).
 *
 * All consumer services (LoanService, TravelService, TurnService, …) call
 * getEffects(playerId) to retrieve the aggregated snapshot, then apply the
 * relevant EffectType values to their own calculations.
 *
 * All sources (social relationships, collections, lifestyle items) implement
 * EffectContributor and are auto-discovered via Spring's List<> injection.
 */
@Service
public class PlayerEffectsService {

    private final List<EffectContributor> contributors;

    public PlayerEffectsService(List<EffectContributor> contributors) {
        this.contributors = contributors;
    }

    /**
     * Aggregates all active effects for the player at the current point in time.
     * Values are additive across sources (e.g. two sources both granting
     * EXPENSE_REDUCTION 0.05 sum to 0.10 total).
     *
     * Call once per request/turn and reuse the snapshot — each call performs DB reads.
     */
    @Transactional(readOnly = true)
    public PlayerEffects getEffects(Long playerId) {
        List<EffectContribution> all = contributors.stream()
            .flatMap(c -> c.getContributions(playerId).stream())
            .toList();
        return PlayerEffects.of(all);
    }
}
