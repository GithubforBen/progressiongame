package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_jobs")
public class PlayerJob {

    @EmbeddedId
    private PlayerJobId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("jobId")
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "months_worked", nullable = false)
    private int monthsWorked = 0;

    @Column(name = "started_at_turn", nullable = false)
    private int startedAtTurn;

    public PlayerJob() {}

    public PlayerJob(Long playerId, Job job, int startedAtTurn) {
        this.id = new PlayerJobId(playerId, job.getId());
        this.job = job;
        this.startedAtTurn = startedAtTurn;
    }

    // --- Getters & Setters ---

    public PlayerJobId getId() { return id; }
    public void setId(PlayerJobId id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getMonthsWorked() { return monthsWorked; }
    public void setMonthsWorked(int monthsWorked) { this.monthsWorked = monthsWorked; }

    public int getStartedAtTurn() { return startedAtTurn; }
    public void setStartedAtTurn(int startedAtTurn) { this.startedAtTurn = startedAtTurn; }
}
