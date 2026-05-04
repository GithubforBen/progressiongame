package com.financegame.dto;

import com.financegame.entity.GameCharacter;
import com.financegame.entity.Player;
import com.financegame.entity.PlayerRealEstate;

import java.math.BigDecimal;
import java.util.List;

public record AdminPlayerDto(
    Long playerId,
    String username,
    BigDecimal cash,
    BigDecimal netWorth,
    int stress,
    int happiness,
    int energy,
    int hunger,
    int schufaScore,
    int jailMonthsRemaining,
    int exileMonthsRemaining,
    boolean burnoutActive,
    int currentTurn,
    List<RealEstateRow> realEstate
) {
    public record RealEstateRow(Long id, String name, String mode, BigDecimal purchasePrice) {}

    public static AdminPlayerDto from(Player player, GameCharacter c, List<PlayerRealEstate> props) {
        List<RealEstateRow> rows = props.stream()
            .map(p -> new RealEstateRow(p.getId(), p.getCatalog().getName(), p.getMode(), p.getPurchasePrice()))
            .toList();
        return new AdminPlayerDto(
            player.getId(), player.getUsername(),
            c.getCash(), c.getNetWorth(),
            c.getStress(), c.getHappiness(), c.getEnergy(), c.getHunger(),
            c.getSchufaScore(), c.getJailMonthsRemaining(), c.getExileMonthsRemaining(),
            c.isBurnoutActive(), c.getCurrentTurn(),
            rows
        );
    }
}
