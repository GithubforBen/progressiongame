package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "player_real_estate")
public class PlayerRealEstate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", nullable = false)
    private RealEstateCatalog catalog;

    @Column(name = "mode", nullable = false, length = 20)
    private String mode = "RENTED_OUT";

    @Column(name = "purchased_at_turn", nullable = false)
    private int purchasedAtTurn;

    @Column(name = "purchase_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public RealEstateCatalog getCatalog() { return catalog; }
    public void setCatalog(RealEstateCatalog catalog) { this.catalog = catalog; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public int getPurchasedAtTurn() { return purchasedAtTurn; }
    public void setPurchasedAtTurn(int purchasedAtTurn) { this.purchasedAtTurn = purchasedAtTurn; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
}
