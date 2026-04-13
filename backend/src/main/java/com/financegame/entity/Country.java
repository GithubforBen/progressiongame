package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "travel_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal travelCost;

    @Column(name = "travel_months", nullable = false)
    private int travelMonths;

    @Column(name = "emoji", length = 10)
    private String emoji;

    @Column(name = "description")
    private String description;

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getTravelCost() { return travelCost; }
    public int getTravelMonths() { return travelMonths; }
    public String getEmoji() { return emoji; }
    public String getDescription() { return description; }
}
