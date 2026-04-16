package com.financegame.controller;

import com.financegame.security.PlayerPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Kept for backwards compatibility. Investment unlocking is now cert-based.
 * Returns empty stub — frontend should use education endpoint instead.
 */
@RestController
@RequestMapping("/api/investment-levels")
public class InvestmentLevelController {

    @GetMapping
    public Map<String, Object> get(@AuthenticationPrincipal PlayerPrincipal principal) {
        return Map.of();
    }
}
