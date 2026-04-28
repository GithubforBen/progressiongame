package com.financegame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "Benutzername darf nicht leer sein")
    @Size(min = 3, max = 50, message = "Benutzername muss zwischen 3 und 50 Zeichen lang sein")
    @Pattern(
        regexp = "^[A-Za-z0-9._-]+$",
        message = "Benutzername darf nur Buchstaben, Ziffern und . _ - enthalten"
    )
    String username,

    @NotBlank(message = "Passwort darf nicht leer sein")
    @Size(min = 8, max = 128, message = "Passwort muss zwischen 8 und 128 Zeichen lang sein")
    String password
) {}
