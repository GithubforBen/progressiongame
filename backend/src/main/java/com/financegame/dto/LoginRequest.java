package com.financegame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank(message = "Benutzername darf nicht leer sein")
    @Size(max = 50, message = "Benutzername darf maximal 50 Zeichen lang sein")
    String username,

    @NotBlank(message = "Passwort darf nicht leer sein")
    @Size(max = 128, message = "Passwort darf maximal 128 Zeichen lang sein")
    String password
) {}
