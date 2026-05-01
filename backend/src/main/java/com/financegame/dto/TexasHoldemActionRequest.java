package com.financegame.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TexasHoldemActionRequest(

    @NotBlank(message = "Aktion erforderlich")
    @Pattern(regexp = "^(FOLD|CALL|CHECK|RAISE)$", message = "Ungueltige Aktion")
    String action,

    @DecimalMin(value = "0.00", message = "Betrag darf nicht negativ sein")
    BigDecimal amount

) {}
