package com.financegame.controller;

import com.financegame.dto.CharacterDto;
import com.financegame.dto.NeedsItemDto;
import com.financegame.repository.NeedsItemRepository;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.CharacterService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/needs")
public class NeedsController {

    private final NeedsItemRepository needsItemRepository;
    private final CharacterService characterService;

    public NeedsController(NeedsItemRepository needsItemRepository, CharacterService characterService) {
        this.needsItemRepository = needsItemRepository;
        this.characterService = characterService;
    }

    @GetMapping("/items")
    public List<NeedsItemDto> getItems() {
        return needsItemRepository.findAll().stream().map(NeedsItemDto::from).toList();
    }

    @PostMapping("/purchase")
    public CharacterDto purchase(
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        String itemId = body.get("itemId");
        return characterService.purchaseNeedItem(principal.id(), itemId);
    }
}
