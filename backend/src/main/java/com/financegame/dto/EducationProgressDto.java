package com.financegame.dto;

import com.financegame.entity.EducationProgress;
import java.util.Arrays;
import java.util.List;

public record EducationProgressDto(
    String mainStage,
    int mainStageMonthsRemaining,
    String mainStageField,
    String sideCert,
    int sideCertMonthsRemaining,
    List<String> completedStages,
    List<AvailableStageDto> availableMainStages,
    List<SideCertDto> availableSideCerts
) {
    /** A stage the player can enroll in right now. */
    public record AvailableStageDto(
        String stageKey,
        String label,
        /** Default duration for this stage; fields may have individual overrides via fieldOptions. */
        int durationMonths,
        boolean requiresField,
        List<FieldOption> fieldOptions,
        /** Enrollment cost in € (deducted on enroll). */
        int cost
    ) {}

    /** A field option within a stage that requires a field selection. */
    public record FieldOption(
        String value,
        String label,
        /** Effective duration for this specific field (may differ from stage default). */
        int durationMonths
    ) {}

    /** A side certification the player can start right now. */
    public record SideCertDto(
        String certKey,
        String label,
        int durationMonths,
        /** Enrollment cost in € (deducted on enroll). */
        int cost
    ) {}

    public static EducationProgressDto from(EducationProgress ep,
                                            List<AvailableStageDto> availableMain,
                                            List<SideCertDto> availableSide) {
        return new EducationProgressDto(
            ep.getMainStage(),
            ep.getMainStageMonthsRemaining(),
            ep.getMainStageField(),
            ep.getSideCert(),
            ep.getSideCertMonthsRemaining(),
            Arrays.asList(ep.getCompletedStages()),
            availableMain,
            availableSide
        );
    }
}
