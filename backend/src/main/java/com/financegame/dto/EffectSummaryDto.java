package com.financegame.dto;

import com.financegame.domain.effect.EffectContribution;
import com.financegame.domain.effect.EffectType;
import com.financegame.domain.effect.PlayerEffects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EffectSummaryDto(List<EffectGroupDto> groups) {

    public record EffectGroupDto(
        String type,
        double total,
        String label,
        String unit,
        List<ContributionDto> contributions
    ) {}

    public record ContributionDto(
        String source,
        String detail,
        double value
    ) {}

    private static final Map<EffectType, String[]> META = new LinkedHashMap<>();
    static {
        // Format: label, unit
        META.put(EffectType.SALARY_MULTIPLIER,          new String[]{"Gehalts-Multiplikator", "%"});
        META.put(EffectType.SALARY_BONUS,               new String[]{"Einkommens-Bonus", "€/Monat"});
        META.put(EffectType.MONTHLY_INCOME_BONUS,       new String[]{"Monatlicher Einkommens-Bonus", "€/Monat"});
        META.put(EffectType.EXPENSE_REDUCTION,          new String[]{"Ausgaben-Reduktion", "%"});
        META.put(EffectType.LOAN_INTEREST_REDUCTION,    new String[]{"Zinssatz-Reduktion", "%-Punkte"});
        META.put(EffectType.PROPERTY_PRICE_DISCOUNT,    new String[]{"Immobilien-Rabatt", "%"});
        META.put(EffectType.TRAVEL_COST_REDUCTION,      new String[]{"Reisekosten-Reduktion", "%"});
        META.put(EffectType.TRAVEL_DURATION_REDUCTION,  new String[]{"Reisedauer-Reduktion", "Monate"});
        META.put(EffectType.COLLECTIBLE_PRICE_DISCOUNT, new String[]{"Sammlerstück-Rabatt", "%"});
        META.put(EffectType.TAX_DETECTION_REDUCTION,    new String[]{"Erkennungsrisiko-Reduktion", "%"});
        META.put(EffectType.STOCK_VOLATILITY_REDUCTION, new String[]{"Aktien-Volatilität-Reduktion", "%"});
        META.put(EffectType.JOB_ACCEPTANCE_BOOST,       new String[]{"Job-Annahme-Boost", "%"});
        META.put(EffectType.GAMBLING_LUCK_BOOST,        new String[]{"Glücksspiel-Glück", "%"});
        META.put(EffectType.COLLECTIBLE_DROP_RATE_BOOST,new String[]{"Event-Spawn-Rate", "%"});
        META.put(EffectType.ROB_SUCCESS_BOOST,          new String[]{"Überfall-Erfolgsboost", "%"});
        META.put(EffectType.ROB_LOOT_MULTIPLIER,        new String[]{"Überfall-Beute-Multiplikator", "%"});
        META.put(EffectType.HUNGER_DECAY_REDUCTION,     new String[]{"Hunger-Abbau-Reduktion", "Punkte/Monat"});
        META.put(EffectType.HAPPINESS_PER_TURN,         new String[]{"Glück pro Monat", "Punkte"});
        META.put(EffectType.STRESS_REDUCTION_PER_TURN,  new String[]{"Stress-Reduktion pro Monat", "Punkte"});
        META.put(EffectType.ENERGY_BONUS_PER_TURN,      new String[]{"Energie-Bonus pro Monat", "Punkte"});
        META.put(EffectType.SCHUFA_BONUS_MONTHLY,       new String[]{"SCHUFA-Bonus pro Monat", "Punkte"});
    }

    public static EffectSummaryDto from(PlayerEffects effects) {
        // Group contributions by EffectType
        Map<EffectType, List<EffectContribution>> grouped = new LinkedHashMap<>();
        for (EffectContribution c : effects.all()) {
            grouped.computeIfAbsent(c.type(), k -> new ArrayList<>()).add(c);
        }

        List<EffectGroupDto> groups = new ArrayList<>();
        // Iterate in META order so display order is consistent
        for (EffectType type : META.keySet()) {
            List<EffectContribution> contribs = grouped.get(type);
            if (contribs == null || contribs.isEmpty()) continue;

            double total = effects.get(type);
            String[] meta = META.get(type);
            String label = meta[0];
            String unit = meta[1];

            // Format total: percentage types shown as %, flat types as raw value
            double displayTotal = unit.equals("%") ? Math.round(total * 1000.0) / 10.0 : Math.round(total * 10.0) / 10.0;

            List<ContributionDto> contributionDtos = contribs.stream()
                .map(c -> {
                    double displayVal = unit.equals("%")
                        ? Math.round(c.value() * 1000.0) / 10.0
                        : Math.round(c.value() * 10.0) / 10.0;
                    return new ContributionDto(c.sourceLabel(), c.sourceDetail(), displayVal);
                })
                .toList();

            groups.add(new EffectGroupDto(type.name(), displayTotal, label, unit, contributionDtos));
        }
        return new EffectSummaryDto(groups);
    }
}
