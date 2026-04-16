package com.financegame.controller;

import com.financegame.dto.CollectibleDto;
import com.financegame.dto.CollectionDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.CollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public List<CollectionDto> getCollections(@AuthenticationPrincipal PlayerPrincipal principal) {
        return collectionService.getPlayerCollections(principal.id());
    }

    @GetMapping("/items")
    public List<CollectibleDto> getItems(@AuthenticationPrincipal PlayerPrincipal principal) {
        return collectionService.getCollectibles(principal.id());
    }

    @PostMapping("/items/{id}/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public CollectibleDto buy(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return collectionService.buyCollectible(principal.id(), id);
    }
}
