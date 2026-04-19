package com.financegame.domain;

import com.financegame.entity.EducationProgress;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerTravel;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.PlayerJobRepository;
import com.financegame.repository.PlayerTravelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class GameContextFactory {

    private final CharacterRepository characterRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final PlayerTravelRepository playerTravelRepository;
    private final PlayerJobRepository playerJobRepository;

    public GameContextFactory(CharacterRepository characterRepository,
                               EducationProgressRepository educationProgressRepository,
                               PlayerTravelRepository playerTravelRepository,
                               PlayerJobRepository playerJobRepository) {
        this.characterRepository = characterRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.playerTravelRepository = playerTravelRepository;
        this.playerJobRepository = playerJobRepository;
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

        return new GameContext(character, completedStages, currentCountry, traveling, activeJobCount);
    }
}
