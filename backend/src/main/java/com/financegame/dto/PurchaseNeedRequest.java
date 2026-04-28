package com.financegame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PurchaseNeedRequest(

    @NotBlank(message = "Item-ID erforderlich")
    @Size(max = 64, message = "Item-ID zu lang")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Item-ID enthaelt ungueltige Zeichen")
    String itemId

) {}
