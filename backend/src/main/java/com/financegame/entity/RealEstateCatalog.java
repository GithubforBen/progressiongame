package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "real_estate_catalog")
public class RealEstateCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150, unique = true)
    private String name;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "purchase_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "monthly_rent", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "rent_savings", nullable = false, precision = 10, scale = 2)
    private BigDecimal rentSavings;

    @Column(name = "required_cert", length = 100)
    private String requiredCert;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(BigDecimal monthlyRent) { this.monthlyRent = monthlyRent; }

    public BigDecimal getRentSavings() { return rentSavings; }
    public void setRentSavings(BigDecimal rentSavings) { this.rentSavings = rentSavings; }

    public String getRequiredCert() { return requiredCert; }
    public void setRequiredCert(String requiredCert) { this.requiredCert = requiredCert; }
}
