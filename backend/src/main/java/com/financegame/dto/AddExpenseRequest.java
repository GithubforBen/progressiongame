package com.financegame.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddExpenseRequest(

    @NotBlank(message = "Kategorie darf nicht leer sein")
    String category,

    @NotBlank(message = "Bezeichnung darf nicht leer sein")
    @Size(max = 100, message = "Bezeichnung darf maximal 100 Zeichen lang sein")
    String label,

    @NotNull(message = "Betrag ist erforderlich")
    @DecimalMin(value = "0.01", message = "Betrag muss groesser als 0 sein")
    @DecimalMax(value = "100000.00", message = "Betrag darf maximal 100.000 betragen")
    BigDecimal amount
) {}
