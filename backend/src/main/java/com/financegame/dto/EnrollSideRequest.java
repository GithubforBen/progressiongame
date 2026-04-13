package com.financegame.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrollSideRequest(
    @NotBlank String cert
) {}
