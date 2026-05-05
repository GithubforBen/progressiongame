package com.financegame.service;

import com.financegame.domain.GameContext;
import com.financegame.domain.condition.HasCertCondition;
import com.financegame.domain.effect.EffectType;
import com.financegame.domain.events.PropertyModeChangedEvent;
import com.financegame.domain.events.PropertyPurchasedEvent;
import com.financegame.dto.ChangeModeRequest;
import com.financegame.dto.PlayerRealEstateDto;
import com.financegame.dto.RealEstateDto;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerRealEstate;
import com.financegame.entity.RealEstateCatalog;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.PlayerRealEstateRepository;
import com.financegame.repository.RealEstateCatalogRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RealEstateService {

    private final RealEstateCatalogRepository catalogRepository;
    private final PlayerRealEstateRepository playerRealEstateRepository;
    private final CharacterService characterService;
    private final EducationProgressRepository educationProgressRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PlayerEffectsService playerEffectsService;

    public RealEstateService(RealEstateCatalogRepository catalogRepository,
                              PlayerRealEstateRepository playerRealEstateRepository,
                              CharacterService characterService,
                              EducationProgressRepository educationProgressRepository,
                              ApplicationEventPublisher eventPublisher,
                              PlayerEffectsService playerEffectsService) {
        this.catalogRepository = catalogRepository;
        this.playerRealEstateRepository = playerRealEstateRepository;
        this.characterService = characterService;
        this.educationProgressRepository = educationProgressRepository;
        this.eventPublisher = eventPublisher;
        this.playerEffectsService = playerEffectsService;
    }

    @Transactional(readOnly = true)
    public List<RealEstateDto> getCatalog(Long playerId) {
        List<String> completedStages = getCompletedStages(playerId);
        Set<Long> ownedCatalogIds = playerRealEstateRepository.findByPlayerId(playerId)
            .stream().map(pre -> pre.getCatalog().getId()).collect(Collectors.toSet());

        return catalogRepository.findAll().stream()
            .map(c -> RealEstateDto.from(c, ownedCatalogIds.contains(c.getId()), completedStages))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerRealEstateDto> getMyProperties(Long playerId) {
        return playerRealEstateRepository.findByPlayerId(playerId)
            .stream().map(PlayerRealEstateDto::from).toList();
    }

    @Transactional
    public PlayerRealEstateDto buy(Long playerId, Long catalogId) {
        RealEstateCatalog catalog = catalogRepository.findById(catalogId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Immobilie nicht gefunden"));

        boolean alreadyOwned = playerRealEstateRepository.findByPlayerId(playerId)
            .stream().anyMatch(pre -> pre.getCatalog().getId().equals(catalogId));
        if (alreadyOwned) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Du besitzt diese Immobilie bereits");
        }

        // Cert-based access check
        String requiredCert = catalog.getRequiredCert();
        if (requiredCert != null) {
            List<String> completedStages = getCompletedStages(playerId);
            GameContext ctx = new GameContext(null, completedStages, null, false, 0, Set.of(), Map.of(), Set.of());
            HasCertCondition certCheck = new HasCertCondition(requiredCert);
            if (!certCheck.isMet(ctx)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, certCheck.describe());
            }
        }

        GameCharacter character = characterService.findOrThrow(playerId);
        double discount = playerEffectsService.getEffects(playerId).get(EffectType.PROPERTY_PRICE_DISCOUNT);
        java.math.BigDecimal finalPrice = discount > 0
            ? catalog.getPurchasePrice().multiply(java.math.BigDecimal.valueOf(1.0 - Math.min(0.5, discount)))
                .setScale(2, java.math.RoundingMode.HALF_UP)
            : catalog.getPurchasePrice();
        characterService.deductCash(playerId, finalPrice, "Immobilienkauf: " + catalog.getName());

        PlayerRealEstate pre = new PlayerRealEstate();
        pre.setPlayerId(playerId);
        pre.setCatalog(catalog);
        pre.setMode("RENTED_OUT");
        pre.setPurchasedAtTurn(character.getCurrentTurn());
        pre.setPurchasePrice(finalPrice);
        playerRealEstateRepository.save(pre);

        characterService.recalculateNetWorth(playerId);

        eventPublisher.publishEvent(
            new PropertyPurchasedEvent(playerId, catalog.getId(), catalog.getName(), finalPrice));

        return PlayerRealEstateDto.from(pre);
    }

    @Transactional
    public PlayerRealEstateDto changeMode(Long playerId, Long propertyId, ChangeModeRequest req) {
        PlayerRealEstate pre = playerRealEstateRepository.findById(propertyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Immobilie nicht gefunden"));

        if (!pre.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Zugriff verweigert");
        }

        String mode = req.mode();
        if (!"SELF_OCCUPIED".equals(mode) && !"RENTED_OUT".equals(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungueltiger Modus");
        }

        pre.setMode(mode);
        playerRealEstateRepository.save(pre);

        eventPublisher.publishEvent(new PropertyModeChangedEvent(playerId, propertyId, mode));

        return PlayerRealEstateDto.from(pre);
    }

    private List<String> getCompletedStages(Long playerId) {
        return educationProgressRepository.findByPlayerId(playerId)
            .map(ep -> Arrays.asList(ep.getCompletedStages()))
            .orElse(List.of());
    }
}
