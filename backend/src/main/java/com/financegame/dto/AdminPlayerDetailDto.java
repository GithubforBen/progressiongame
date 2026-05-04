package com.financegame.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminPlayerDetailDto(
    List<CollectibleRow>   collectibles,
    TravelRow              travel,
    List<SocialRow>        socialRelationships,
    List<InvestmentRow>    investments,
    List<String>           completedEducationStages
) {
    public record CollectibleRow(
        String name,
        String rarity,
        String collectionName,
        String country
    ) {}

    public record TravelRow(
        String currentCountry,
        String destinationCountry,
        Integer arriveAtTurn,
        List<String> visitedCountries
    ) {}

    public record SocialRow(
        String personId,
        String personName,
        int score
    ) {}

    public record InvestmentRow(
        String name,
        String type,
        BigDecimal amountInvested,
        BigDecimal currentValue
    ) {}
}
