package com.financegame.controller;

import com.financegame.dto.LifestyleItemDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.LifestyleService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lifestyle")
public class LifestyleController {

    private final LifestyleService lifestyleService;

    public LifestyleController(LifestyleService lifestyleService) {
        this.lifestyleService = lifestyleService;
    }

    @GetMapping
    public List<LifestyleItemDto> getCatalog(@AuthenticationPrincipal PlayerPrincipal principal) {
        return lifestyleService.getCatalog(principal.id());
    }

    @PostMapping("/buy/{itemId}")
    @ResponseStatus(HttpStatus.CREATED)
    public LifestyleItemDto buyItem(
        @PathVariable String itemId,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return lifestyleService.buyItem(principal.id(), itemId);
    }
}
