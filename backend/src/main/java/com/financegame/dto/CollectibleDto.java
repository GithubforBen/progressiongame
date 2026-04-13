package com.financegame.dto;

import com.financegame.entity.Collectible;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record CollectibleDto(
    Long id,
    String name,
    String collectionType,
    String countryRequired,
    String rarity,
    BigDecimal baseValue,
    BigDecimal effectivePrice,
    String description,
    boolean canBuy,
    boolean alreadyOwned,
    boolean onSale
) {
    public static CollectibleDto from(Collectible c, boolean canBuy, boolean alreadyOwned, boolean onSale) {
        BigDecimal effectivePrice = onSale
            ? c.getBaseValue().multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP)
            : c.getBaseValue();
        return new CollectibleDto(
            c.getId(), c.getName(), c.getCollectionType(), c.getCountryRequired(),
            c.getRarity(), c.getBaseValue(), effectivePrice,
            c.getDescription(), canBuy, alreadyOwned, onSale
        );
    }
}
