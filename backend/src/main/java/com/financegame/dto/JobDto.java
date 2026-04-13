package com.financegame.dto;

import com.financegame.entity.Job;
import java.math.BigDecimal;

public record JobDto(
    Long id,
    String name,
    String description,
    String requiredEducationType,
    String requiredEducationField,
    int requiredMonthsExperience,
    BigDecimal salary,
    int stressPerMonth,
    // Player-specific flags
    boolean meetsRequirements,
    boolean alreadyApplied,
    boolean alreadyWorking
) {
    public static JobDto from(Job job, boolean meetsRequirements,
                              boolean alreadyApplied, boolean alreadyWorking) {
        return new JobDto(
            job.getId(),
            job.getName(),
            job.getDescription(),
            job.getRequiredEducationType(),
            job.getRequiredEducationField(),
            job.getRequiredMonthsExperience(),
            job.getSalary(),
            job.getStressPerMonth(),
            meetsRequirements,
            alreadyApplied,
            alreadyWorking
        );
    }
}
