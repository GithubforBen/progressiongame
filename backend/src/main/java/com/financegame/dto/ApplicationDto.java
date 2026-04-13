package com.financegame.dto;

import com.financegame.entity.JobApplication;
import java.math.BigDecimal;

public record ApplicationDto(
    Long id,
    Long jobId,
    String jobName,
    BigDecimal jobSalary,
    int appliedAtTurn,
    String status,
    Integer resolvedAtTurn
) {
    public static ApplicationDto from(JobApplication a) {
        return new ApplicationDto(
            a.getId(),
            a.getJob().getId(),
            a.getJob().getName(),
            a.getJob().getSalary(),
            a.getAppliedAtTurn(),
            a.getStatus(),
            a.getResolvedAtTurn()
        );
    }
}
