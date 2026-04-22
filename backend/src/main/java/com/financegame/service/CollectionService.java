package com.financegame.service;

import com.financegame.domain.GameContext;
import com.financegame.domain.condition.HasCertCondition;
import com.financegame.domain.events.CollectiblePurchasedEvent;
import com.financegame.dto.CollectibleDto;
import com.financegame.dto.CollectionDto;
import com.financegame.dto.PublicCollectionDto;
import com.financegame.entity.Collection;
import com.financegame.entity.Collectible;
import com.financegame.entity.PlayerCollectible;
import com.financegame.entity.PlayerTravel;
import com.financegame.repository.CollectibleRepository;
import com.financegame.repository.CollectionRepository;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.PlayerCollectibleRepository;
import com.financegame.repository.PlayerTravelRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    /**
     * Simple record for an active bonus derived from a completed collection.
     */
    public record ActiveBonus(String bonusType, BigDecimal bonusValue) {}

    private final CollectionRepository collectionRepository;
    private final CollectibleRepository collectibleRepository;
    private final PlayerCollectibleRepository playerCollectibleRepository;
    private final PlayerTravelRepository playerTravelRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final CharacterService characterService;
    private final ApplicationEventPublisher eventPublisher;

    public CollectionService(
        CollectionRepository collectionRepository,
        CollectibleRepository collectibleRepository,
        PlayerCollectibleRepository playerCollectibleRepository,
        PlayerTravelRepository playerTravelRepository,
        EducationProgressRepository educationProgressRepository,
        CharacterService characterService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.collectionRepository = collectionRepository;
        this.collectibleRepository = collectibleRepository;
        this.playerCollectibleRepository = playerCollectibleRepository;
        this.playerTravelRepository = playerTravelRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.characterService = characterService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<CollectionDto> getPlayerCollections(Long playerId) {
        List<Collection> all = collectionRepository.findAll();
        List<String> completedStages = getCompletedStages(playerId);
        Set<Long> ownedIds = playerCollectibleRepository.findByPlayerId(playerId)
            .stream().map(PlayerCollectible::getCollectibleId).collect(Collectors.toSet());

        // Count owned items per collection
        Map<String, Long> ownedPerCollection = collectibleRepository.findAll().stream()
            .filter(c -> c.getCollectionName() != null && ownedIds.contains(c.getId()))
            .collect(Collectors.groupingBy(Collectible::getCollectionName, Collectors.counting()));

        return all.stream().map(col -> {
            int owned = ownedPerCollection.getOrDefault(col.getName(), 0L).intValue();
            boolean completed = owned >= col.getItemCount();
            String req = col.getRequiredCert();
            boolean locked = req != null && !completedStages.contains(req);
            return new CollectionDto(
                col.getName(),
                col.getDisplayName(),
                col.getBonusType(),
                col.getBonusValue(),
                col.getItemCount(),
                owned,
                completed,
                req,
                locked
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicCollectionDto> getPublicCollections(Long playerId) {
        List<Collection> all = collectionRepository.findAll();
        Set<Long> ownedIds = playerCollectibleRepository.findByPlayerId(playerId)
            .stream().map(PlayerCollectible::getCollectibleId).collect(Collectors.toSet());
        Map<String, Long> ownedPerCollection = collectibleRepository.findAll().stream()
            .filter(c -> c.getCollectionName() != null && ownedIds.contains(c.getId()))
            .collect(Collectors.groupingBy(Collectible::getCollectionName, Collectors.counting()));
        return all.stream()
            .map(col -> {
                int owned = ownedPerCollection.getOrDefault(col.getName(), 0L).intValue();
                return new PublicCollectionDto(
                    col.getName(), col.getDisplayName(),
                    col.getItemCount(), owned, owned >= col.getItemCount()
                );
            })
            .sorted(java.util.Comparator.comparingInt(PublicCollectionDto::ownedCount).reversed())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CollectibleDto> getCollectibles(Long playerId) {
        List<String> completedStages = getCompletedStages(playerId);
        Set<Long> ownedIds = playerCollectibleRepository.findByPlayerId(playerId)
            .stream().map(PlayerCollectible::getCollectibleId).collect(Collectors.toSet());

        String currentCountry = playerTravelRepository.findByPlayerId(playerId)
            .map(PlayerTravel::getCurrentCountry).orElse(null);

        // Build a map of collectionName → requiredCert for cert-gating collectibles by collection
        Map<String, String> collectionCerts = collectionRepository.findAll().stream()
            .filter(c -> c.getRequiredCert() != null)
            .collect(Collectors.toMap(Collection::getName, Collection::getRequiredCert));

        return collectibleRepository.findAll().stream()
            .map(c -> {
                boolean owned = ownedIds.contains(c.getId());
                boolean travelOk = isTravelAccessible(c, currentCountry);
                // Check collection cert requirement
                String colCert = c.getCollectionName() != null ? collectionCerts.get(c.getCollectionName()) : null;
                boolean certOk = colCert == null || completedStages.contains(colCert);
                boolean canBuy = !owned && travelOk && certOk;
                return CollectibleDto.forShop(c, owned, canBuy, currentCountry);
            })
            .toList();
    }

    @Transactional
    public CollectibleDto buyCollectible(Long playerId, Long collectibleId) {
        Collectible item = collectibleRepository.findById(collectibleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Sammelgegenstand nicht gefunden"));

        if (playerCollectibleRepository.existsById(playerId, collectibleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Du besitzt diesen Sammelgegenstand bereits");
        }

        String currentCountry = playerTravelRepository.findByPlayerId(playerId)
            .map(PlayerTravel::getCurrentCountry).orElse(null);

        if (!isTravelAccessible(item, currentCountry)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du musst in " + item.getCountryRequired() + " sein, um diesen Gegenstand zu kaufen");
        }

        // Check collection cert requirement
        if (item.getCollectionName() != null) {
            Collection col = collectionRepository.findByName(item.getCollectionName()).orElse(null);
            if (col != null && col.getRequiredCert() != null) {
                List<String> completedStages = getCompletedStages(playerId);
                GameContext ctx = new GameContext(null, completedStages, null, false, 0, Set.of(), Map.of(), Set.of());
                HasCertCondition certCheck = new HasCertCondition(col.getRequiredCert());
                if (!certCheck.isMet(ctx)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, certCheck.describe());
                }
            }
        }

        BigDecimal price = (item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) > 0)
            ? item.getPrice() : item.getBaseValue();
        characterService.deductCash(playerId, price, "Kauf: " + item.getName());

        var character = characterService.findOrThrow(playerId);
        PlayerCollectible pc = new PlayerCollectible(playerId, collectibleId,
            character.getCurrentTurn(), price);
        playerCollectibleRepository.save(pc);

        characterService.recalculateNetWorth(playerId);

        eventPublisher.publishEvent(new CollectiblePurchasedEvent(
            playerId, collectibleId, item.getName(), item.getCollectionName(), price));

        return CollectibleDto.forShop(item, true, false, currentCountry);
    }

    /** An item is accessible if it has no country requirement, requires Germany (home), or the player is there. */
    private boolean isTravelAccessible(Collectible c, String currentCountry) {
        String req = c.getCountryRequired();
        if (req == null || req.isBlank() || req.equals("Deutschland")) return true;
        return req.equals(currentCountry);
    }

    /**
     * Returns all active bonuses for the player based on completed collections.
     * Called by TurnService each turn.
     */
    @Transactional(readOnly = true)
    public List<ActiveBonus> getActiveCollectionBonuses(Long playerId) {
        List<CollectionDto> collections = getPlayerCollections(playerId);
        return collections.stream()
            .filter(CollectionDto::completed)
            .map(col -> new ActiveBonus(col.bonusType(), col.bonusValue()))
            .toList();
    }

    private List<String> getCompletedStages(Long playerId) {
        return educationProgressRepository.findByPlayerId(playerId)
            .map(ep -> Arrays.asList(ep.getCompletedStages()))
            .orElse(List.of());
    }
}
