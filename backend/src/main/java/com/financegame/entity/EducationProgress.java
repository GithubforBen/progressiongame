package com.financegame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "education_progress")
public class EducationProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, unique = true)
    private Long playerId;

    @Column(name = "main_stage", nullable = false, length = 50)
    private String mainStage = "GRUNDSCHULE";

    @Column(name = "main_stage_months_remaining", nullable = false)
    private int mainStageMonthsRemaining = 0;

    @Column(name = "main_stage_field", length = 50)
    private String mainStageField;

    @Column(name = "side_cert", length = 50)
    private String sideCert;

    @Column(name = "side_cert_months_remaining", nullable = false)
    private int sideCertMonthsRemaining = 0;

    // PostgreSQL TEXT[] stored as String[] via Hibernate 6 array support
    @Column(name = "completed_stages", columnDefinition = "TEXT[]")
    private String[] completedStages = {"GRUNDSCHULE"};

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getMainStage() { return mainStage; }
    public void setMainStage(String mainStage) { this.mainStage = mainStage; }

    public int getMainStageMonthsRemaining() { return mainStageMonthsRemaining; }
    public void setMainStageMonthsRemaining(int v) { this.mainStageMonthsRemaining = v; }

    public String getMainStageField() { return mainStageField; }
    public void setMainStageField(String mainStageField) { this.mainStageField = mainStageField; }

    public String getSideCert() { return sideCert; }
    public void setSideCert(String sideCert) { this.sideCert = sideCert; }

    public int getSideCertMonthsRemaining() { return sideCertMonthsRemaining; }
    public void setSideCertMonthsRemaining(int v) { this.sideCertMonthsRemaining = v; }

    public String[] getCompletedStages() { return completedStages; }
    public void setCompletedStages(String[] completedStages) { this.completedStages = completedStages; }
}
