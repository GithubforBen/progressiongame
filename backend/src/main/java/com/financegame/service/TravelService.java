package com.financegame.service;

import com.financegame.domain.GameContext;
import com.financegame.domain.condition.NotInJailCondition;
import com.financegame.domain.condition.NotTravelingCondition;
import com.financegame.domain.events.TravelDepartedEvent;
import com.financegame.dto.CountryDto;
import com.financegame.dto.PlayerTravelStatusDto;
import com.financegame.entity.Country;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerTravel;
import com.financegame.repository.CountryRepository;
import com.financegame.repository.PlayerTravelRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TravelService {

    private final CountryRepository countryRepository;
    private final PlayerTravelRepository playerTravelRepository;
    private final CharacterService characterService;
    private final ApplicationEventPublisher eventPublisher;

    public TravelService(CountryRepository countryRepository,
                         PlayerTravelRepository playerTravelRepository,
                         CharacterService characterService,
                         ApplicationEventPublisher eventPublisher) {
        this.countryRepository = countryRepository;
        this.playerTravelRepository = playerTravelRepository;
        this.characterService = characterService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<CountryDto> getCountries(Long playerId) {
        PlayerTravel travel = getOrCreate(playerId);
        return countryRepository.findAll().stream()
            .map(c -> CountryDto.from(c, travel.getCurrentCountry(),
                travel.getDestinationCountry(), travel.getVisitedCountries()))
            .toList();
    }

    @Transactional(readOnly = true)
    public PlayerTravelStatusDto getStatus(Long playerId) {
        PlayerTravel travel = getOrCreate(playerId);
        GameCharacter character = characterService.findOrThrow(playerId);
        return PlayerTravelStatusDto.from(travel, character);
    }

    @Transactional
    public PlayerTravelStatusDto depart(Long playerId, String countryName) {
        GameCharacter character = characterService.findOrThrow(playerId);
        PlayerTravel travel = getOrCreate(playerId);
        GameContext ctx = new GameContext(character, List.of(), travel.getCurrentCountry(), travel.isTraveling(), 0);

        if (!new NotInJailCondition().isMet(ctx)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du kannst nicht reisen — du sitzt in Haft (" + character.getJailMonthsRemaining() + " Monate verbleiben)");
        }

        Country country = countryRepository.findByName(countryName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Land nicht gefunden: " + countryName));

        if (!new NotTravelingCondition().isMet(ctx)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du bist bereits auf Reisen nach " + travel.getDestinationCountry());
        }
        if (countryName.equals(travel.getCurrentCountry())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du bist bereits in " + countryName);
        }
        if (character.getExileMonthsRemaining() > 0 && isHomeCountry(countryName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du befindest dich im Exil und kannst noch nicht nach Deutschland zurückkehren ("
                    + character.getExileMonthsRemaining() + " Monate verbleiben)");
        }

        characterService.deductCash(playerId, country.getTravelCost(), "Flug nach " + countryName);

        travel.setDestinationCountry(countryName);
        travel.setArriveAtTurn(character.getCurrentTurn() + country.getTravelMonths());
        travel.setCurrentCountry(null);
        playerTravelRepository.save(travel);

        eventPublisher.publishEvent(
            new TravelDepartedEvent(playerId, countryName, travel.getArriveAtTurn()));

        return PlayerTravelStatusDto.from(travel, character);
    }

    @Transactional
    public PlayerTravelStatusDto returnHome(Long playerId) {
        GameCharacter character = characterService.findOrThrow(playerId);
        if (character.getExileMonthsRemaining() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du befindest dich im Exil — Rückkehr erst in "
                    + character.getExileMonthsRemaining() + " Monaten möglich");
        }
        PlayerTravel travel = getOrCreate(playerId);
        if (travel.isTraveling()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Reise erst abwarten bevor du heimkehrst");
        }
        travel.setCurrentCountry(null);
        playerTravelRepository.save(travel);
        return PlayerTravelStatusDto.from(travel, character);
    }

    public PlayerTravel getOrCreate(Long playerId) {
        return playerTravelRepository.findByPlayerId(playerId)
            .orElseGet(() -> {
                PlayerTravel t = new PlayerTravel(playerId);
                playerTravelRepository.save(t);
                return t;
            });
    }

    private boolean isHomeCountry(String countryName) {
        return "Deutschland".equalsIgnoreCase(countryName);
    }
}
