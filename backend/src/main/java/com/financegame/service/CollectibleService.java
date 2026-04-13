package com.financegame.service;

import com.financegame.dto.ActiveEventDto;
import com.financegame.dto.CollectibleDto;
import com.financegame.dto.PlayerCollectibleDto;
import com.financegame.entity.*;
import com.financegame.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CollectibleService {

    private final CollectibleRepository collectibleRepository;
    private final PlayerCollectibleRepository playerCollectibleRepository;
    private final ActiveEventRepository activeEventRepository;
    private final PlayerTravelRepository playerTravelRepository;
    private final CharacterService characterService;

    public CollectibleService(CollectibleRepository collectibleRepository,
                               PlayerCollectibleRepository playerCollectibleRepository,
                               ActiveEventRepository activeEventRepository,
                               PlayerTravelRepository playerTravelRepository,
                               CharacterService characterService) {
        this.collectibleRepository = collectibleRepository;
        this.playerCollectibleRepository = playerCollectibleRepository;
        this.activeEventRepository = activeEventRepository;
        this.playerTravelRepository = playerTravelRepository;
        this.characterService = characterService;
    }

    @Transactional(readOnly = true)
    public List<CollectibleDto> getAllCollectibles(Long playerId) {
        GameCharacter character = characterService.findOrThrow(playerId);
        PlayerTravel travel = playerTravelRepository.findByPlayerId(playerId).orElse(null);
        String currentCountry = travel != null ? travel.getCurrentCountry() : null;

        Set<Long> owned = playerCollectibleRepository.findByPlayerId(playerId)
            .stream().map(PlayerCollectible::getCollectibleId).collect(Collectors.toSet());

        List<ActiveEvent> saleEvents = activeEventRepository.findActiveForPlayer(playerId, character.getCurrentTurn());
        Set<Long> onSaleIds = saleEvents.stream()
            .filter(e -> "COLLECTIBLE_SALE".equals(e.getType()) && e.getCollectibleId() != null)
            .map(ActiveEvent::getCollectibleId)
            .collect(Collectors.toSet());

        return collectibleRepository.findAll().stream().map(c -> {
            boolean alreadyOwned = owned.contains(c.getId());
            boolean onSale = onSaleIds.contains(c.getId());
            // Can buy if: in the right country OR on sale (event ships it to you)
            boolean canBuy = !alreadyOwned && (c.getCountryRequired().equals(currentCountry) || onSale);
            return CollectibleDto.from(c, canBuy, alreadyOwned, onSale);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerCollectibleDto> getMyCollection(Long playerId) {
        return playerCollectibleRepository.findByPlayerId(playerId).stream()
            .map(pc -> {
                Collectible c = collectibleRepository.findById(pc.getCollectibleId())
                    .orElseThrow();
                return PlayerCollectibleDto.from(pc, c);
            }).toList();
    }

    @Transactional(readOnly = true)
    public List<ActiveEventDto> getActiveEvents(Long playerId) {
        GameCharacter character = characterService.findOrThrow(playerId);
        return activeEventRepository.findActiveForPlayer(playerId, character.getCurrentTurn())
            .stream()
            .map(e -> {
                String name = e.getCollectibleId() != null
                    ? collectibleRepository.findById(e.getCollectibleId())
                        .map(Collectible::getName).orElse("Unbekannt")
                    : null;
                return ActiveEventDto.from(e, name);
            }).toList();
    }

    @Transactional
    public PlayerCollectibleDto buyCollectible(Long playerId, Long collectibleId) {
        GameCharacter character = characterService.findOrThrow(playerId);

        if (playerCollectibleRepository.existsById(playerId, collectibleId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Du besitzt dieses Sammlerstück bereits");
        }

        Collectible c = collectibleRepository.findById(collectibleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sammlerstück nicht gefunden"));

        PlayerTravel travel = playerTravelRepository.findByPlayerId(playerId).orElse(null);
        String currentCountry = travel != null ? travel.getCurrentCountry() : null;

        List<ActiveEvent> saleEvents = activeEventRepository.findActiveForPlayer(playerId, character.getCurrentTurn());
        boolean onSale = saleEvents.stream()
            .anyMatch(e -> "COLLECTIBLE_SALE".equals(e.getType()) && collectibleId.equals(e.getCollectibleId()));

        boolean canBuy = c.getCountryRequired().equals(currentCountry) || onSale;
        if (!canBuy) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du musst in " + c.getCountryRequired() + " sein um dieses Sammlerstück zu kaufen");
        }

        java.math.BigDecimal price = onSale
            ? c.getBaseValue().multiply(java.math.BigDecimal.valueOf(0.70))
                .setScale(2, java.math.RoundingMode.HALF_UP)
            : c.getBaseValue();

        characterService.deductCash(playerId, price, "Sammlerstück: " + c.getName());

        PlayerCollectible pc = new PlayerCollectible(playerId, collectibleId, character.getCurrentTurn(), price);
        playerCollectibleRepository.save(pc);

        return PlayerCollectibleDto.from(pc, c);
    }
}
