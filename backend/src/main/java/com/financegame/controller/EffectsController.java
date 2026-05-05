package com.financegame.controller;

import com.financegame.dto.EffectSummaryDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.PlayerEffectsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/effects")
public class EffectsController {

    private final PlayerEffectsService playerEffectsService;

    public EffectsController(PlayerEffectsService playerEffectsService) {
        this.playerEffectsService = playerEffectsService;
    }

    @GetMapping
    public EffectSummaryDto getEffects(@AuthenticationPrincipal PlayerPrincipal principal) {
        return EffectSummaryDto.from(playerEffectsService.getEffects(principal.id()));
    }
}
