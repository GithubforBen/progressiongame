package com.financegame.dto;

public record AuthResponse(
    String token,
    UserDto user
) {
    public record UserDto(Long id, String username) {}
}
