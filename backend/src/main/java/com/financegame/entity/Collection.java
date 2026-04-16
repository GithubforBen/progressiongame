package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "bonus_type", nullable = false, length = 50)
    private String bonusType;

    @Column(name = "bonus_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal bonusValue;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "required_cert", length = 100)
    private String requiredCert;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBonusType() { return bonusType; }
    public void setBonusType(String bonusType) { this.bonusType = bonusType; }

    public BigDecimal getBonusValue() { return bonusValue; }
    public void setBonusValue(BigDecimal bonusValue) { this.bonusValue = bonusValue; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public String getRequiredCert() { return requiredCert; }
    public void setRequiredCert(String requiredCert) { this.requiredCert = requiredCert; }
}
