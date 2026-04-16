package com.financegame.dto;

import com.financegame.entity.Job;
import java.math.BigDecimal;

public record JobDto(
    Long id,
    String name,
    String description,
    String category,
    String requiredEducationType,
    String requiredEducationField,
    /** Human-readable German label for the required side certification (nullable). */
    String requiredSideCert,
    int requiredMonthsExperience,
    BigDecimal salary,
    int stressPerMonth,
    int maxParallel,
    // Player-specific flags
    boolean meetsRequirements,
    boolean alreadyApplied,
    boolean alreadyWorking
) {
    public static JobDto from(Job job, boolean meetsRequirements,
                              boolean alreadyApplied, boolean alreadyWorking,
                              String sideCertDisplayName) {
        return new JobDto(
            job.getId(),
            job.getName(),
            job.getDescription(),
            job.getCategory(),
            job.getRequiredEducationType(),
            job.getRequiredEducationField(),
            sideCertDisplayName,
            job.getRequiredMonthsExperience(),
            job.getSalary(),
            job.getStressPerMonth(),
            job.getMaxParallel(),
            meetsRequirements,
            alreadyApplied,
            alreadyWorking
        );
    }
}
