package com.financegame.dto;

import com.financegame.entity.ActiveEvent;

public record ActiveEventDto(
    Long id,
    String type,
    String country,
    int expiresAtTurn,
    Long collectibleId,
    String message
) {
    public static ActiveEventDto from(ActiveEvent e, String collectibleName) {
        String msg = switch (e.getType()) {
            case "COLLECTIBLE_SALE" -> "30% Rabatt auf \"" + collectibleName + "\" in " + e.getCountry()
                + "! Endet in Monat " + e.getExpiresAtTurn() + ".";
            case "TRAVEL_BONUS" -> "Sonderangebot: Reise nach " + e.getCountry()
                + " diese Monat zum halben Preis!";
            default -> e.getType();
        };
        return new ActiveEventDto(e.getId(), e.getType(), e.getCountry(),
            e.getExpiresAtTurn(), e.getCollectibleId(), msg);
    }
}
