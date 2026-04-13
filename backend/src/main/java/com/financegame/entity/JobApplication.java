package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "applied_at_turn", nullable = false)
    private int appliedAtTurn;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "resolved_at_turn")
    private Integer resolvedAtTurn;

    public JobApplication() {}

    public JobApplication(Long playerId, Job job, int appliedAtTurn) {
        this.playerId = playerId;
        this.job = job;
        this.appliedAtTurn = appliedAtTurn;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public int getAppliedAtTurn() { return appliedAtTurn; }
    public void setAppliedAtTurn(int t) { this.appliedAtTurn = t; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getResolvedAtTurn() { return resolvedAtTurn; }
    public void setResolvedAtTurn(Integer t) { this.resolvedAtTurn = t; }
}
