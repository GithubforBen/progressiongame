package com.financegame.dto;

import com.financegame.entity.RealEstateCatalog;

import java.math.BigDecimal;
import java.util.List;

public record RealEstateDto(
    Long id,
    String name,
    String location,
    String category,
    String description,
    BigDecimal purchasePrice,
    BigDecimal monthlyRent,
    BigDecimal rentSavings,
    String requiredCert,
    boolean owned,
    boolean locked
) {
    public static RealEstateDto from(RealEstateCatalog c, boolean owned, List<String> completedStages) {
        String req = c.getRequiredCert();
        boolean locked = req != null && !completedStages.contains(req);
        return new RealEstateDto(
            c.getId(),
            c.getName(),
            c.getLocation(),
            c.getCategory(),
            c.getDescription(),
            c.getPurchasePrice(),
            c.getMonthlyRent(),
            c.getRentSavings(),
            req,
            owned,
            locked
        );
    }
}
