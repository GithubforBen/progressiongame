package com.financegame.dto;

import com.financegame.entity.PlayerRealEstate;

import java.math.BigDecimal;

public record PlayerRealEstateDto(
    Long id,
    Long catalogId,
    String name,
    String location,
    String category,
    String description,
    String mode,
    int purchasedAtTurn,
    BigDecimal purchasePrice,
    BigDecimal monthlyRent,
    BigDecimal rentSavings
) {
    public static PlayerRealEstateDto from(PlayerRealEstate pre) {
        var c = pre.getCatalog();
        return new PlayerRealEstateDto(
            pre.getId(),
            c.getId(),
            c.getName(),
            c.getLocation(),
            c.getCategory(),
            c.getDescription(),
            pre.getMode(),
            pre.getPurchasedAtTurn(),
            pre.getPurchasePrice(),
            c.getMonthlyRent(),
            c.getRentSavings()
        );
    }
}
