package com.financegame.dto;

import com.financegame.entity.PlayerTravel;

public record PlayerTravelStatusDto(
    String currentCountry,
    String destinationCountry,
    Integer arriveAtTurn,
    boolean traveling,
    String[] visitedCountries
) {
    public static PlayerTravelStatusDto from(PlayerTravel t) {
        return new PlayerTravelStatusDto(
            t.getCurrentCountry(),
            t.getDestinationCountry(),
            t.getArriveAtTurn(),
            t.isTraveling(),
            t.getVisitedCountries()
        );
    }
}
