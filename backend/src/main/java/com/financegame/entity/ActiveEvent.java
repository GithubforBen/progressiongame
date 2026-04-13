package com.financegame.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "active_events")
public class ActiveEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "type", nullable = false, length = 30)
    private String type; // COLLECTIBLE_SALE, TRAVEL_BONUS

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "expires_at_turn", nullable = false)
    private int expiresAtTurn;

    @Column(name = "collectible_id")
    private Long collectibleId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ActiveEvent() {}

    public Long getId() { return id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public int getExpiresAtTurn() { return expiresAtTurn; }
    public void setExpiresAtTurn(int expiresAtTurn) { this.expiresAtTurn = expiresAtTurn; }

    public Long getCollectibleId() { return collectibleId; }
    public void setCollectibleId(Long collectibleId) { this.collectibleId = collectibleId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
