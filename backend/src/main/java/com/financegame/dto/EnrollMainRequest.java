package com.financegame.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrollMainRequest(
    @NotBlank String stage,
    String field   // required when stage has field variants (AUSBILDUNG, BACHELOR, MASTER)
) {}
