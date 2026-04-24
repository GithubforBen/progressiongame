package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lifestyle_item_catalog")
public class LifestyleItemCatalog {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 10)
    private String icon;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cost;

    @Column(name = "monthly_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyCost;

    @Column(name = "stress_reduction_month", nullable = false)
    private int stressReductionMonth;

    @Column(name = "tax_evasion_boost", nullable = false)
    private boolean taxEvasionBoost;

    @Column(name = "unlocks_billionaire", nullable = false)
    private boolean unlocksBillionaire;

    @Column(columnDefinition = "TEXT")
    private String description;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getMonthlyCost() { return monthlyCost; }
    public void setMonthlyCost(BigDecimal monthlyCost) { this.monthlyCost = monthlyCost; }
    public int getStressReductionMonth() { return stressReductionMonth; }
    public void setStressReductionMonth(int v) { this.stressReductionMonth = v; }
    public boolean isTaxEvasionBoost() { return taxEvasionBoost; }
    public void setTaxEvasionBoost(boolean v) { this.taxEvasionBoost = v; }
    public boolean isUnlocksBillionaire() { return unlocksBillionaire; }
    public void setUnlocksBillionaire(boolean v) { this.unlocksBillionaire = v; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
