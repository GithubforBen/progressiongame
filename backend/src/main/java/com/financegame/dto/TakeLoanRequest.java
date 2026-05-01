package com.financegame.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TakeLoanRequest(

    @Size(max = 100, message = "Bezeichnung darf maximal 100 Zeichen lang sein")
    String label,

    @NotNull(message = "Betrag ist erforderlich")
    @DecimalMin(value = "1000.00", message = "Mindestbetrag: 1.000 €")
    BigDecimal amount,

    @Min(value = 6, message = "Laufzeit muss mindestens 6 Monate betragen")
    @Max(value = 360, message = "Laufzeit darf maximal 360 Monate betragen")
    int termMonths
) {}
