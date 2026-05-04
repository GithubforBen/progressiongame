package com.financegame.service;

import com.financegame.dto.AdminCharacterEditRequest;
import com.financegame.dto.AdminPlayerDetailDto;
import com.financegame.dto.AdminPlayerDto;
import com.financegame.entity.Collectible;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.Player;
import com.financegame.entity.PlayerRealEstate;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.CollectibleRepository;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.InvestmentRepository;
import com.financegame.repository.PlayerCollectibleRepository;
import com.financegame.repository.PlayerRealEstateRepository;
import com.financegame.repository.PlayerRepository;
import com.financegame.repository.PlayerSocialRelationshipRepository;
import com.financegame.repository.PlayerTravelRepository;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.PersonService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final String adminUsername;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final PlayerRealEstateRepository realEstateRepository;
    private final CharacterService characterService;
    private final PlayerCollectibleRepository playerCollectibleRepository;
    private final CollectibleRepository collectibleRepository;
    private final PlayerTravelRepository playerTravelRepository;
    private final PlayerSocialRelationshipRepository socialRelationshipRepository;
    private final InvestmentRepository investmentRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final PersonService personService;

    public AdminService(
        @Value("${admin.username:}") String adminUsername,
        PlayerRepository playerRepository,
        CharacterRepository characterRepository,
        PlayerRealEstateRepository realEstateRepository,
        CharacterService characterService,
        PlayerCollectibleRepository playerCollectibleRepository,
        CollectibleRepository collectibleRepository,
        PlayerTravelRepository playerTravelRepository,
        PlayerSocialRelationshipRepository socialRelationshipRepository,
        InvestmentRepository investmentRepository,
        EducationProgressRepository educationProgressRepository,
        PersonService personService
    ) {
        this.adminUsername = adminUsername;
        this.playerRepository = playerRepository;
        this.characterRepository = characterRepository;
        this.realEstateRepository = realEstateRepository;
        this.characterService = characterService;
        this.playerCollectibleRepository = playerCollectibleRepository;
        this.collectibleRepository = collectibleRepository;
        this.playerTravelRepository = playerTravelRepository;
        this.socialRelationshipRepository = socialRelationshipRepository;
        this.investmentRepository = investmentRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.personService = personService;
    }

    public boolean isAdmin(PlayerPrincipal principal) {
        return !adminUsername.isBlank() && adminUsername.equals(principal.username());
    }

    public void requireAdmin(PlayerPrincipal principal) {
        if (!isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nicht autorisiert");
        }
    }

    @Transactional(readOnly = true)
    public List<AdminPlayerDto> listAllPlayers() {
        return playerRepository.findAll().stream()
            .flatMap(player -> characterRepository.findByPlayerId(player.getId())
                .map(character -> {
                    List<PlayerRealEstate> props = realEstateRepository.findByPlayerId(player.getId());
                    return AdminPlayerDto.from(player, character, props);
                })
                .stream())
            .toList();
    }

    @Transactional
    public AdminPlayerDto editCharacter(Long playerId, AdminCharacterEditRequest req) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spieler nicht gefunden"));
        GameCharacter character = characterRepository.findByPlayerId(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charakter nicht gefunden"));

        if (req.cash() != null)                 character.setCash(req.cash());
        if (req.stress() != null)               character.setStress(clamp(req.stress(), 0, 100));
        if (req.happiness() != null)            character.setHappiness(clamp(req.happiness(), 0, 100));
        if (req.energy() != null)               character.setEnergy(clamp(req.energy(), 0, 100));
        if (req.hunger() != null)               character.setHunger(clamp(req.hunger(), 0, 100));
        if (req.schufaScore() != null)          character.setSchufaScore(clamp(req.schufaScore(), 0, 1000));
        if (req.jailMonthsRemaining() != null)  character.setJailMonthsRemaining(Math.max(0, req.jailMonthsRemaining()));
        if (req.exileMonthsRemaining() != null) character.setExileMonthsRemaining(Math.max(0, req.exileMonthsRemaining()));
        if (req.burnoutActive() != null)        character.setBurnoutActive(req.burnoutActive());

        characterRepository.save(character);

        // Recompute net worth so it reflects any cash change immediately
        characterService.recalculateNetWorth(playerId);

        GameCharacter updated = characterRepository.findByPlayerId(playerId).orElseThrow();
        List<PlayerRealEstate> props = realEstateRepository.findByPlayerId(playerId);
        return AdminPlayerDto.from(player, updated, props);
    }

    @Transactional
    public void deleteRealEstate(Long playerId, Long realEstateId) {
        PlayerRealEstate prop = realEstateRepository.findById(realEstateId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Immobilie nicht gefunden"));
        if (!prop.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Immobilie gehört nicht diesem Spieler");
        }
        realEstateRepository.delete(realEstateId);
        characterService.recalculateNetWorth(playerId);
    }

    @Transactional(readOnly = true)
    public AdminPlayerDetailDto getPlayerDetails(Long playerId) {
        // Collectibles
        Map<Long, Collectible> catalogById = collectibleRepository.findAll().stream()
            .collect(Collectors.toMap(Collectible::getId, c -> c));
        List<AdminPlayerDetailDto.CollectibleRow> collectibles = playerCollectibleRepository.findByPlayerId(playerId)
            .stream()
            .map(pc -> {
                Collectible c = catalogById.get(pc.getCollectibleId());
                if (c == null) return null;
                return new AdminPlayerDetailDto.CollectibleRow(
                    c.getName(), c.getRarity(), c.getCollectionName(), c.getCountryRequired());
            })
            .filter(r -> r != null)
            .sorted(java.util.Comparator.comparing(AdminPlayerDetailDto.CollectibleRow::collectionName,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                .thenComparing(AdminPlayerDetailDto.CollectibleRow::name))
            .toList();

        // Travel
        AdminPlayerDetailDto.TravelRow travel = playerTravelRepository.findByPlayerId(playerId)
            .map(pt -> new AdminPlayerDetailDto.TravelRow(
                pt.getCurrentCountry(),
                pt.getDestinationCountry(),
                pt.getArriveAtTurn(),
                pt.getVisitedCountries() != null ? Arrays.asList(pt.getVisitedCountries()) : List.of()))
            .orElse(new AdminPlayerDetailDto.TravelRow("Deutschland", null, null, List.of()));

        // Social relationships
        List<AdminPlayerDetailDto.SocialRow> social = socialRelationshipRepository.findByPlayerId(playerId)
            .stream()
            .map(rel -> {
                String name = personService.findById(rel.getPersonId())
                    .map(p -> p.name())
                    .orElse(rel.getPersonId());
                return new AdminPlayerDetailDto.SocialRow(rel.getPersonId(), name, rel.getScore());
            })
            .sorted(java.util.Comparator.comparingInt(AdminPlayerDetailDto.SocialRow::score).reversed())
            .toList();

        // Investments
        List<AdminPlayerDetailDto.InvestmentRow> investments = investmentRepository.findByPlayerId(playerId)
            .stream()
            .map(inv -> new AdminPlayerDetailDto.InvestmentRow(
                inv.getName(), inv.getType(), inv.getAmountInvested(), inv.getCurrentValue()))
            .sorted(java.util.Comparator.comparing(AdminPlayerDetailDto.InvestmentRow::currentValue).reversed())
            .toList();

        // Education
        List<String> educationStages = educationProgressRepository.findByPlayerId(playerId)
            .map(ep -> Arrays.asList(ep.getCompletedStages()))
            .orElse(List.of());

        return new AdminPlayerDetailDto(collectibles, travel, social, investments, educationStages);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
