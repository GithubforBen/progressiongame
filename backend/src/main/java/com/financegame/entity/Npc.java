package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "npcs")
public class Npc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "personality", nullable = false)
    private String personality;

    @Column(name = "happiness_bonus_per_level", nullable = false)
    private int happinessBonusPerLevel;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPersonality() { return personality; }
    public int getHappinessBonusPerLevel() { return happinessBonusPerLevel; }
}
