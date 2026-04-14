package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "player_loans")
public class PlayerLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "amount_borrowed", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountBorrowed;

    @Column(name = "amount_remaining", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountRemaining;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "monthly_payment", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "turns_remaining", nullable = false)
    private int turnsRemaining;

    @Column(name = "taken_at_turn", nullable = false)
    private int takenAtTurn;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getAmountBorrowed() { return amountBorrowed; }
    public void setAmountBorrowed(BigDecimal amountBorrowed) { this.amountBorrowed = amountBorrowed; }

    public BigDecimal getAmountRemaining() { return amountRemaining; }
    public void setAmountRemaining(BigDecimal amountRemaining) { this.amountRemaining = amountRemaining; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    public int getTurnsRemaining() { return turnsRemaining; }
    public void setTurnsRemaining(int turnsRemaining) { this.turnsRemaining = turnsRemaining; }

    public int getTakenAtTurn() { return takenAtTurn; }
    public void setTakenAtTurn(int takenAtTurn) { this.takenAtTurn = takenAtTurn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
