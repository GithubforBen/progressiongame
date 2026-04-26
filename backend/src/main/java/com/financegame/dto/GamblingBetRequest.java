package com.financegame.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GamblingBetRequest(

    @NotNull(message = "Einsatz ist erforderlich")
    @DecimalMin(value = "1.00", message = "Mindesteinsatz: 1,00 €")
    @DecimalMax(value = "10000.00", message = "Maximaleinsatz: 10.000,00 €")
    BigDecimal bet
) {}
