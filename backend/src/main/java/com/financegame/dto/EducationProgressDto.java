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
    /** A stage the player can enroll in right now */
    public record AvailableStageDto(
        String stageKey,
        String label,
        int durationMonths,
        boolean requiresField,
        List<FieldOption> fieldOptions
    ) {}

    public record FieldOption(String value, String label) {}

    public record SideCertDto(
        String certKey,
        String label,
        int durationMonths
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
