package com.financegame.domain;

import com.financegame.dto.CollectionDto;
import com.financegame.entity.EducationProgress;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerSocialRelationship;
import com.financegame.entity.PlayerTravel;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.PlayerJobRepository;
import com.financegame.repository.PlayerSocialRelationshipRepository;
import com.financegame.repository.PlayerTravelRepository;
import com.financegame.service.CollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameContextFactory {

    private final CharacterRepository characterRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final PlayerTravelRepository playerTravelRepository;
    private final PlayerJobRepository playerJobRepository;
    private final CollectionService collectionService;
    private final PlayerSocialRelationshipRepository socialRelationshipRepository;

    public GameContextFactory(CharacterRepository characterRepository,
                               EducationProgressRepository educationProgressRepository,
                               PlayerTravelRepository playerTravelRepository,
                               PlayerJobRepository playerJobRepository,
                               CollectionService collectionService,
                               PlayerSocialRelationshipRepository socialRelationshipRepository) {
        this.characterRepository = characterRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.playerTravelRepository = playerTravelRepository;
        this.playerJobRepository = playerJobRepository;
        this.collectionService = collectionService;
        this.socialRelationshipRepository = socialRelationshipRepository;
    }

    public GameContext build(Long playerId) {
        GameCharacter character = characterRepository.findByPlayerId(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character not found"));

        List<String> completedStages = educationProgressRepository.findByPlayerId(playerId)
            .map(EducationProgress::getCompletedStages)
            .map(Arrays::asList)
            .orElse(List.of());

        PlayerTravel travel = playerTravelRepository.findByPlayerId(playerId).orElse(null);
        String currentCountry = (travel != null) ? travel.getCurrentCountry() : null;
        boolean traveling = (travel != null) && travel.isTraveling();

        int activeJobCount = playerJobRepository.findActiveByPlayerId(playerId).size();

        Set<String> completedCollections = collectionService.getPlayerCollections(playerId)
            .stream()
            .filter(CollectionDto::completed)
            .map(CollectionDto::name)
            .collect(Collectors.toSet());

        List<PlayerSocialRelationship> relationships = socialRelationshipRepository.findByPlayerId(playerId);

        Map<String, Integer> relationshipScores = relationships.stream()
            .collect(Collectors.toMap(
                PlayerSocialRelationship::getPersonId,
                PlayerSocialRelationship::getScore
            ));

        Set<String> hadConflictsWith = relationships.stream()
            .filter(PlayerSocialRelationship::isHadConflict)
            .map(PlayerSocialRelationship::getPersonId)
            .collect(Collectors.toSet());

        return new GameContext(character, completedStages, currentCountry, traveling, activeJobCount,
            completedCollections, relationshipScores, hadConflictsWith);
    }
}
