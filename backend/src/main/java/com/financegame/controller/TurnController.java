package com.financegame.controller;

import com.financegame.dto.TurnResultDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.TurnService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/turn")
public class TurnController {

    private final TurnService turnService;

    public TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    @PostMapping("/end")
    public TurnResultDto endTurn(@AuthenticationPrincipal PlayerPrincipal principal) {
        return turnService.endTurn(principal.id());
    }
}
