package com.financegame.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank(message = "Benutzername darf nicht leer sein")
    String username,

    @NotBlank(message = "Passwort darf nicht leer sein")
    String password
) {}
