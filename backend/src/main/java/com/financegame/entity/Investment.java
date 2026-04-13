package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "investments")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "type", nullable = false, length = 30)
    private String type; // STOCK, REAL_ESTATE, ART, NFT, COMPANY, COLLECTIBLE

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "amount_invested", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountInvested;

    @Column(name = "current_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 6)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "acquired_at_turn", nullable = false)
    private int acquiredAtTurn;

    @Column(name = "stock_id")
    private Long stockId;

    public Investment() {}

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmountInvested() { return amountInvested; }
    public void setAmountInvested(BigDecimal amountInvested) { this.amountInvested = amountInvested; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public int getAcquiredAtTurn() { return acquiredAtTurn; }
    public void setAcquiredAtTurn(int acquiredAtTurn) { this.acquiredAtTurn = acquiredAtTurn; }

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
}
