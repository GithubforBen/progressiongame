package com.financegame.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BuyStockRequest(

    @NotBlank(message = "Ticker darf nicht leer sein")
    @Size(max = 20, message = "Ticker darf maximal 20 Zeichen lang sein")
    @Pattern(regexp = "^[A-Z0-9_.-]+$", message = "Ticker enthaelt ungueltiger Zeichen")
    String ticker,

    @NotNull(message = "Menge ist erforderlich")
    @DecimalMin(value = "0.000001", message = "Menge muss groesser als 0 sein")
    BigDecimal quantity
) {}
