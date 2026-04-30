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

    @Column(name = "depression_months_remaining", nullable = false)
    private int depressionMonthsRemaining = 0;

    @Column(name = "burnout_active", nullable = false)
    private boolean burnoutActive = false;

    @Column(name = "tax_evasion_active", nullable = false)
    private boolean taxEvasionActive = false;

    @Column(name = "tax_evasion_caught_pending", nullable = false)
    private boolean taxEvasionCaughtPending = false;

    @Column(name = "cumulative_evaded_taxes", nullable = false, precision = 15, scale = 2)
    private BigDecimal cumulativeEvadedTaxes = BigDecimal.ZERO;

    @Column(name = "jail_months_remaining", nullable = false)
    private int jailMonthsRemaining = 0;

    @Column(name = "exile_months_remaining", nullable = false)
    private int exileMonthsRemaining = 0;

    @Column(name = "total_jail_months_served", nullable = false)
    private int totalJailMonthsServed = 0;

    @Column(name = "finanzamt_audit_months_remaining", nullable = false)
    private int finanzamtAuditMonthsRemaining = 0;

    @Column(name = "victory_achieved", nullable = false)
    private boolean victoryAchieved = false;

    @Column(name = "personal_best_net_worth", nullable = false, precision = 20, scale = 2)
    private BigDecimal personalBestNetWorth = BigDecimal.ZERO;

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

    public int getDepressionMonthsRemaining() { return depressionMonthsRemaining; }
    public void setDepressionMonthsRemaining(int v) { this.depressionMonthsRemaining = v; }

    public boolean isBurnoutActive() { return burnoutActive; }
    public void setBurnoutActive(boolean burnoutActive) { this.burnoutActive = burnoutActive; }

    public boolean isTaxEvasionActive() { return taxEvasionActive; }
    public void setTaxEvasionActive(boolean taxEvasionActive) { this.taxEvasionActive = taxEvasionActive; }

    public boolean isTaxEvasionCaughtPending() { return taxEvasionCaughtPending; }
    public void setTaxEvasionCaughtPending(boolean taxEvasionCaughtPending) { this.taxEvasionCaughtPending = taxEvasionCaughtPending; }

    public BigDecimal getCumulativeEvadedTaxes() { return cumulativeEvadedTaxes; }
    public void setCumulativeEvadedTaxes(BigDecimal cumulativeEvadedTaxes) { this.cumulativeEvadedTaxes = cumulativeEvadedTaxes; }

    public int getJailMonthsRemaining() { return jailMonthsRemaining; }
    public void setJailMonthsRemaining(int jailMonthsRemaining) { this.jailMonthsRemaining = jailMonthsRemaining; }

    public int getExileMonthsRemaining() { return exileMonthsRemaining; }
    public void setExileMonthsRemaining(int exileMonthsRemaining) { this.exileMonthsRemaining = exileMonthsRemaining; }

    public int getTotalJailMonthsServed() { return totalJailMonthsServed; }
    public void setTotalJailMonthsServed(int totalJailMonthsServed) { this.totalJailMonthsServed = totalJailMonthsServed; }

    public int getFinanzamtAuditMonthsRemaining() { return finanzamtAuditMonthsRemaining; }
    public void setFinanzamtAuditMonthsRemaining(int v) { this.finanzamtAuditMonthsRemaining = v; }

    public boolean isVictoryAchieved() { return victoryAchieved; }
    public void setVictoryAchieved(boolean victoryAchieved) { this.victoryAchieved = victoryAchieved; }

    public BigDecimal getPersonalBestNetWorth() { return personalBestNetWorth; }
    public void setPersonalBestNetWorth(BigDecimal personalBestNetWorth) { this.personalBestNetWorth = personalBestNetWorth; }
}
