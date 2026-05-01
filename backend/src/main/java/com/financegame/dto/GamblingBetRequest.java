package com.financegame.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GamblingBetRequest(

    @NotNull(message = "Einsatz ist erforderlich")
    @DecimalMin(value = "1.00", message = "Mindesteinsatz: 1,00 €")
    BigDecimal bet
) {}
