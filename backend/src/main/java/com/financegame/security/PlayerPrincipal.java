package com.financegame.security;

/**
 * Represents the authenticated player in the SecurityContext.
 * Accessible in controllers via @AuthenticationPrincipal PlayerPrincipal.
 */
public record PlayerPrincipal(Long id, String username) {}
