package com.financegame.controller;

import com.financegame.dto.ActiveEventDto;
import com.financegame.dto.CollectibleDto;
import com.financegame.dto.CollectionProgressDto;
import com.financegame.dto.PlayerCollectibleDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.CollectibleService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collectibles")
public class CollectibleController {

    private final CollectibleService collectibleService;

    public CollectibleController(CollectibleService collectibleService) {
        this.collectibleService = collectibleService;
    }

    @GetMapping
    public List<CollectibleDto> getAll(@AuthenticationPrincipal PlayerPrincipal principal) {
        return collectibleService.getAllCollectibles(principal.id());
    }

    @GetMapping("/my")
    public List<PlayerCollectibleDto> getMyCollection(@AuthenticationPrincipal PlayerPrincipal principal) {
        return collectibleService.getMyCollection(principal.id());
    }

    @GetMapping("/events")
    public List<ActiveEventDto> getActiveEvents(@AuthenticationPrincipal PlayerPrincipal principal) {
        return collectibleService.getActiveEvents(principal.id());
    }

    @GetMapping("/progress")
    public List<CollectionProgressDto> getProgress(@AuthenticationPrincipal PlayerPrincipal principal) {
        return collectibleService.getCollectionProgress(principal.id());
    }

    @PostMapping("/{id}/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerCollectibleDto buy(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return collectibleService.buyCollectible(principal.id(), id);
    }
}
