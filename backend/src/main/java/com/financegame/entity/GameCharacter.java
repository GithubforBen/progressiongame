package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "characters")
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, unique = true)
    private Long playerId;

    @Column(name = "cash", nullable = false, precision = 15, scale = 2)
    private BigDecimal cash = new BigDecimal("1000.00");

    @Column(name = "net_worth", nullable = false, precision = 15, scale = 2)
    private BigDecimal netWorth = new BigDecimal("1000.00");

    @Column(name = "stress", nullable = false)
    private int stress = 0;

    @Column(name = "hunger", nullable = false)
    private int hunger = 100;

    @Column(name = "energy", nullable = false)
    private int energy = 100;

    @Column(name = "happiness", nullable = false)
    private int happiness = 70;

    @Column(name = "current_turn", nullable = false)
    private int currentTurn = 1;

    @Column(name = "schufa_score", nullable = false)
    private int schufaScore = 500;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public BigDecimal getCash() { return cash; }
    public void setCash(BigDecimal cash) { this.cash = cash; }

    public BigDecimal getNetWorth() { return netWorth; }
    public void setNetWorth(BigDecimal netWorth) { this.netWorth = netWorth; }

    public int getStress() { return stress; }
    public void setStress(int stress) { this.stress = stress; }

    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = hunger; }

    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = energy; }

    public int getHappiness() { return happiness; }
    public void setHappiness(int happiness) { this.happiness = happiness; }

    public int getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(int currentTurn) { this.currentTurn = currentTurn; }

    public int getSchufaScore() { return schufaScore; }
    public void setSchufaScore(int schufaScore) { this.schufaScore = schufaScore; }
}
