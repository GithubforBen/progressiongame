package com.financegame.dto;

import com.financegame.entity.PlayerJob;
import java.math.BigDecimal;

public record PlayerJobDto(
    Long jobId,
    String jobName,
    BigDecimal salary,
    int stressPerMonth,
    int monthsWorked,
    int startedAtTurn
) {
    public static PlayerJobDto from(PlayerJob pj) {
        return new PlayerJobDto(
            pj.getJob().getId(),
            pj.getJob().getName(),
            pj.getJob().getSalary(),
            pj.getJob().getStressPerMonth(),
            pj.getMonthsWorked(),
            pj.getStartedAtTurn()
        );
    }
}
