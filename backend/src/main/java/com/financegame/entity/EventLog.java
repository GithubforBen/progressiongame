package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events_log")
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount_effect", precision = 10, scale = 2)
    private BigDecimal amountEffect;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "created_at_turn", nullable = false)
    private int createdAtTurn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public EventLog() {}

    public EventLog(Long playerId, String description, BigDecimal amountEffect,
                    String eventType, int createdAtTurn) {
        this.playerId = playerId;
        this.description = description;
        this.amountEffect = amountEffect;
        this.eventType = eventType;
        this.createdAtTurn = createdAtTurn;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public String getDescription() { return description; }
    public BigDecimal getAmountEffect() { return amountEffect; }
    public String getEventType() { return eventType; }
    public int getCreatedAtTurn() { return createdAtTurn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
