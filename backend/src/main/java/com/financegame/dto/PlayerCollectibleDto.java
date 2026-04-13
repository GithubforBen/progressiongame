package com.financegame.dto;

import com.financegame.entity.Collectible;
import com.financegame.entity.PlayerCollectible;
import java.math.BigDecimal;

public record PlayerCollectibleDto(
    Long collectibleId,
    String name,
    String collectionType,
    String countryRequired,
    String rarity,
    BigDecimal baseValue,
    BigDecimal purchasePrice,
    int acquiredAtTurn
) {
    public static PlayerCollectibleDto from(PlayerCollectible pc, Collectible c) {
        return new PlayerCollectibleDto(
            c.getId(), c.getName(), c.getCollectionType(), c.getCountryRequired(),
            c.getRarity(), c.getBaseValue(), pc.getPurchasePrice(), pc.getAcquiredAtTurn()
        );
    }
}
