package com.financegame.dto;

import com.financegame.entity.RealEstateCatalog;

import java.math.BigDecimal;

public record RealEstateDto(
    Long id,
    String name,
    String location,
    String category,
    String description,
    BigDecimal purchasePrice,
    BigDecimal monthlyRent,
    BigDecimal rentSavings,
    boolean owned
) {
    public static RealEstateDto from(RealEstateCatalog c, boolean owned) {
        return new RealEstateDto(
            c.getId(),
            c.getName(),
            c.getLocation(),
            c.getCategory(),
            c.getDescription(),
            c.getPurchasePrice(),
            c.getMonthlyRent(),
            c.getRentSavings(),
            owned
        );
    }
}
