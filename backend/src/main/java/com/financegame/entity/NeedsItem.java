package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "needs_items")
public class NeedsItem {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "hunger_effect", nullable = false)
    private int hungerEffect = 0;

    @Column(name = "energy_effect", nullable = false)
    private int energyEffect = 0;

    @Column(name = "happiness_effect", nullable = false)
    private int happinessEffect = 0;

    @Column(name = "stress_effect", nullable = false)
    private int stressEffect = 0;

    @Column(name = "depression_reduction", nullable = false)
    private boolean depressionReduction = false;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getHungerEffect() { return hungerEffect; }
    public void setHungerEffect(int hungerEffect) { this.hungerEffect = hungerEffect; }

    public int getEnergyEffect() { return energyEffect; }
    public void setEnergyEffect(int energyEffect) { this.energyEffect = energyEffect; }

    public int getHappinessEffect() { return happinessEffect; }
    public void setHappinessEffect(int happinessEffect) { this.happinessEffect = happinessEffect; }

    public int getStressEffect() { return stressEffect; }
    public void setStressEffect(int stressEffect) { this.stressEffect = stressEffect; }

    public boolean isDepressionReduction() { return depressionReduction; }
    public void setDepressionReduction(boolean depressionReduction) { this.depressionReduction = depressionReduction; }
}
