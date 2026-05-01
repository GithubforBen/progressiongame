package com.financegame.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record RouletteRequest(

    @NotNull
    @NotEmpty(message = "Mindestens eine Wette erforderlich")
    @Size(max = 50, message = "Maximal 50 Wetten")
    @Valid
    List<RouletteBet> bets

) {
    public record RouletteBet(

        @NotBlank(message = "Wetttyp erforderlich")
        String type,

        @NotNull
        List<@Min(0) @Max(36) Integer> numbers,

        @NotNull
        @DecimalMin(value = "1.00", message = "Mindesteinsatz pro Wette: 1,00 €")
        BigDecimal amount

    ) {}
}
