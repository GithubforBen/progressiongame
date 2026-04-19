package com.financegame.dto;

import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerTravel;

public record PlayerTravelStatusDto(
    String currentCountry,
    String destinationCountry,
    Integer arriveAtTurn,
    boolean traveling,
    String[] visitedCountries,
    boolean inJail,
    int jailMonthsRemaining,
    boolean inExile,
    int exileMonthsRemaining
) {
    public static PlayerTravelStatusDto from(PlayerTravel t, GameCharacter character) {
        return new PlayerTravelStatusDto(
            t.getCurrentCountry(),
            t.getDestinationCountry(),
            t.getArriveAtTurn(),
            t.isTraveling(),
            t.getVisitedCountries(),
            character.getJailMonthsRemaining() > 0,
            character.getJailMonthsRemaining(),
            character.getExileMonthsRemaining() > 0,
            character.getExileMonthsRemaining()
        );
    }

    /** Fallback without character — jail/exile fields default to false/0. */
    public static PlayerTravelStatusDto from(PlayerTravel t) {
        return new PlayerTravelStatusDto(
            t.getCurrentCountry(),
            t.getDestinationCountry(),
            t.getArriveAtTurn(),
            t.isTraveling(),
            t.getVisitedCountries(),
            false, 0, false, 0
        );
    }
}
