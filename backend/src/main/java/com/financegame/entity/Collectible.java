package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "collectibles")
public class Collectible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "collection_type", nullable = false, length = 50)
    private String collectionType;

    @Column(name = "country_required", nullable = false, length = 50)
    private String countryRequired;

    @Column(name = "rarity", nullable = false, length = 20)
    private String rarity; // COMMON, RARE, EPIC, LEGENDARY

    @Column(name = "base_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseValue;

    @Column(name = "description")
    private String description;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCollectionType() { return collectionType; }
    public String getCountryRequired() { return countryRequired; }
    public String getRarity() { return rarity; }
    public BigDecimal getBaseValue() { return baseValue; }
    public String getDescription() { return description; }
}
