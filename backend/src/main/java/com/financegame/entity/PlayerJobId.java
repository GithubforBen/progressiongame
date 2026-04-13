package com.financegame.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PlayerJobId implements Serializable {

    private Long playerId;
    private Long jobId;

    public PlayerJobId() {}

    public PlayerJobId(Long playerId, Long jobId) {
        this.playerId = playerId;
        this.jobId = jobId;
    }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerJobId that)) return false;
        return Objects.equals(playerId, that.playerId) && Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() { return Objects.hash(playerId, jobId); }
}
