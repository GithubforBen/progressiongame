package com.financegame.service;

import com.financegame.dto.ChangeModeRequest;
import com.financegame.dto.PlayerRealEstateDto;
import com.financegame.dto.RealEstateDto;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerRealEstate;
import com.financegame.entity.RealEstateCatalog;
import com.financegame.repository.PlayerRealEstateRepository;
import com.financegame.repository.RealEstateCatalogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RealEstateService {

    private final RealEstateCatalogRepository catalogRepository;
    private final PlayerRealEstateRepository playerRealEstateRepository;
    private final CharacterService characterService;

    public RealEstateService(RealEstateCatalogRepository catalogRepository,
                              PlayerRealEstateRepository playerRealEstateRepository,
                              CharacterService characterService) {
        this.catalogRepository = catalogRepository;
        this.playerRealEstateRepository = playerRealEstateRepository;
        this.characterService = characterService;
    }

    @Transactional(readOnly = true)
    public List<RealEstateDto> getCatalog(Long playerId) {
        Set<Long> ownedCatalogIds = playerRealEstateRepository.findByPlayerId(playerId)
            .stream().map(pre -> pre.getCatalog().getId()).collect(Collectors.toSet());

        return catalogRepository.findAll().stream()
            .map(c -> RealEstateDto.from(c, ownedCatalogIds.contains(c.getId())))
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

        GameCharacter character = characterService.findOrThrow(playerId);
        characterService.deductCash(playerId, catalog.getPurchasePrice(), "Immobilienkauf: " + catalog.getName());

        PlayerRealEstate pre = new PlayerRealEstate();
        pre.setPlayerId(playerId);
        pre.setCatalog(catalog);
        pre.setMode("RENTED_OUT");
        pre.setPurchasedAtTurn(character.getCurrentTurn());
        pre.setPurchasePrice(catalog.getPurchasePrice());
        playerRealEstateRepository.save(pre);

        characterService.recalculateNetWorth(playerId);

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
        return PlayerRealEstateDto.from(pre);
    }
}
