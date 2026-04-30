package com.financegame.controller;

import com.financegame.dto.CharacterDto;
import com.financegame.dto.NeedsItemDto;
import com.financegame.dto.PurchaseNeedRequest;
import com.financegame.entity.PlayerNeedsUsage;
import com.financegame.repository.NeedsItemRepository;
import com.financegame.repository.PlayerNeedsUsageRepository;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.CharacterService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/needs")
public class NeedsController {

    private final NeedsItemRepository needsItemRepository;
    private final PlayerNeedsUsageRepository usageRepository;
    private final CharacterService characterService;

    public NeedsController(NeedsItemRepository needsItemRepository,
                           PlayerNeedsUsageRepository usageRepository,
                           CharacterService characterService) {
        this.needsItemRepository = needsItemRepository;
        this.usageRepository = usageRepository;
        this.characterService = characterService;
    }

    @GetMapping("/items")
    public List<NeedsItemDto> getItems(@AuthenticationPrincipal PlayerPrincipal principal) {
        int currentTurn = characterService.findOrThrow(principal.id()).getCurrentTurn();

        // Build a map of itemId → lastUsedTurn for this player
        Map<String, Integer> lastUsed = usageRepository.findByPlayerId(principal.id()).stream()
            .collect(Collectors.toMap(u -> u.getId().getItemId(), PlayerNeedsUsage::getLastUsedTurn));

        return needsItemRepository.findAll().stream().map(item -> {
            int remaining = 0;
            if (item.getCooldownTurns() > 0 && lastUsed.containsKey(item.getId())) {
                int turnsAgo = currentTurn - lastUsed.get(item.getId());
                remaining = Math.max(0, item.getCooldownTurns() - turnsAgo);
            }
            return NeedsItemDto.from(item, remaining);
        }).toList();
    }

    @PostMapping("/purchase")
    public CharacterDto purchase(
        @Valid @RequestBody PurchaseNeedRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return characterService.purchaseNeedItem(principal.id(), request.itemId());
    }
}
