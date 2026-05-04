package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "ticker", nullable = false, unique = true, length = 10)
    private String ticker;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // NORMAL, MEME, ETF, DIVIDEND_STOCK, BOND, REIT, CRYPTO, LEVERAGE, WARRANT, SHORT, FUTURES

    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "required_cert", length = 100)
    private String requiredCert;

    @Column(name = "initial_price", nullable = false)
    private BigDecimal initialPrice;

    // history JSONB column exists in DB but is managed via stock_price_history table

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public String getRequiredCert() { return requiredCert; }
    public void setRequiredCert(String requiredCert) { this.requiredCert = requiredCert; }

    public BigDecimal getInitialPrice() { return initialPrice; }
    public void setInitialPrice(BigDecimal initialPrice) { this.initialPrice = initialPrice; }
}
