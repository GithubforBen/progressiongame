package com.financegame.dto;

import com.financegame.entity.Collectible;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record CollectibleDto(
    Long id,
    String name,
    String collectionName,
    String collectionType,
    String countryRequired,
    String rarity,
    BigDecimal baseValue,
    BigDecimal effectivePrice,
    BigDecimal shopPrice,
    String description,
    boolean canBuy,
    boolean alreadyOwned,
    boolean onSale
) {
    /** Used by the travel/events system. */
    public static CollectibleDto from(Collectible c, boolean canBuy, boolean alreadyOwned, boolean onSale) {
        BigDecimal effectivePrice = onSale
            ? c.getBaseValue().multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP)
            : c.getBaseValue();
        return new CollectibleDto(
            c.getId(), c.getName(), c.getCollectionName(), c.getCollectionType(), c.getCountryRequired(),
            c.getRarity(), c.getBaseValue(), effectivePrice, c.getPrice(),
            c.getDescription(), canBuy, alreadyOwned, onSale
        );
    }

    /** Used by the collections shop — includes travel-based canBuy logic. */
    public static CollectibleDto forShop(Collectible c, boolean owned, boolean canBuy, String currentCountry) {
        return new CollectibleDto(
            c.getId(), c.getName(), c.getCollectionName(), c.getCollectionType(), c.getCountryRequired(),
            c.getRarity(), c.getBaseValue(), c.getPrice(), c.getPrice(),
            c.getDescription(), canBuy, owned, false
        );
    }

    /** Backwards-compat overload (no travel context — assumes Germany/home). */
    public static CollectibleDto forShop(Collectible c, boolean owned) {
        String req = c.getCountryRequired();
        boolean canBuy = !owned && (req == null || req.isBlank() || req.equals("Deutschland"));
        return forShop(c, owned, canBuy, null);
    }
}
