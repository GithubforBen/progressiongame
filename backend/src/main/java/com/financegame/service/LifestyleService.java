package com.financegame.service;

import com.financegame.dto.LifestyleItemDto;
import com.financegame.entity.LifestyleItemCatalog;
import com.financegame.entity.PlayerLifestyleItem;
import com.financegame.repository.LifestyleItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LifestyleService {

    private final LifestyleItemRepository repo;
    private final CharacterService characterService;

    public LifestyleService(LifestyleItemRepository repo, CharacterService characterService) {
        this.repo = repo;
        this.characterService = characterService;
    }

    @Transactional(readOnly = true)
    public List<LifestyleItemDto> getCatalog(Long playerId) {
        Set<String> owned = repo.findByPlayerId(playerId)
            .stream().map(PlayerLifestyleItem::getItemId).collect(Collectors.toSet());
        return repo.findAllCatalog().stream()
            .map(c -> LifestyleItemDto.from(c, owned.contains(c.getId())))
            .toList();
    }

    @Transactional
    public LifestyleItemDto buyItem(Long playerId, String itemId) {
        LifestyleItemCatalog item = repo.findCatalogById(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item nicht gefunden"));

        if (repo.existsByPlayerIdAndItemId(playerId, itemId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bereits besessen");
        }

        characterService.deductCash(playerId, item.getCost(), "Lifestyle: " + item.getName());

        int turn = characterService.findOrThrow(playerId).getCurrentTurn();
        repo.save(new PlayerLifestyleItem(playerId, itemId, turn));

        return LifestyleItemDto.from(item, true);
    }

    @Transactional(readOnly = true)
    public List<LifestyleItemCatalog> getOwnedCatalogItems(Long playerId) {
        Set<String> ownedIds = repo.findByPlayerId(playerId)
            .stream().map(PlayerLifestyleItem::getItemId).collect(Collectors.toSet());
        return repo.findAllCatalog().stream()
            .filter(c -> ownedIds.contains(c.getId()))
            .toList();
    }
}
