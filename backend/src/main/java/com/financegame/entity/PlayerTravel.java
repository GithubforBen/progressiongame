package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_travel")
public class PlayerTravel {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "current_country")
    private String currentCountry; // null = zuhause

    @Column(name = "destination_country")
    private String destinationCountry;

    @Column(name = "arrive_at_turn")
    private Integer arriveAtTurn;

    @Column(name = "visited_countries", columnDefinition = "TEXT[]")
    private String[] visitedCountries = {};

    public PlayerTravel() {}

    public PlayerTravel(Long playerId) {
        this.playerId = playerId;
        this.visitedCountries = new String[]{};
    }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getCurrentCountry() { return currentCountry; }
    public void setCurrentCountry(String currentCountry) { this.currentCountry = currentCountry; }

    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }

    public Integer getArriveAtTurn() { return arriveAtTurn; }
    public void setArriveAtTurn(Integer arriveAtTurn) { this.arriveAtTurn = arriveAtTurn; }

    public String[] getVisitedCountries() { return visitedCountries != null ? visitedCountries : new String[]{}; }
    public void setVisitedCountries(String[] visitedCountries) { this.visitedCountries = visitedCountries; }

    public boolean isTraveling() { return destinationCountry != null; }
}
