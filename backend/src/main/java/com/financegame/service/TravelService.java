package com.financegame.service;

import com.financegame.dto.CountryDto;
import com.financegame.dto.PlayerTravelStatusDto;
import com.financegame.entity.Country;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerTravel;
import com.financegame.repository.CountryRepository;
import com.financegame.repository.PlayerTravelRepository;
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

    public TravelService(CountryRepository countryRepository,
                         PlayerTravelRepository playerTravelRepository,
                         CharacterService characterService) {
        this.countryRepository = countryRepository;
        this.playerTravelRepository = playerTravelRepository;
        this.characterService = characterService;
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
        return PlayerTravelStatusDto.from(getOrCreate(playerId));
    }

    @Transactional
    public PlayerTravelStatusDto depart(Long playerId, String countryName) {
        Country country = countryRepository.findByName(countryName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Land nicht gefunden: " + countryName));

        PlayerTravel travel = getOrCreate(playerId);

        if (travel.isTraveling()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du bist bereits auf Reisen nach " + travel.getDestinationCountry());
        }
        if (countryName.equals(travel.getCurrentCountry())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Du bist bereits in " + countryName);
        }

        // Check active TRAVEL_BONUS event for half price
        // (simplified: no event discount in depart — handled in frontend display)
        characterService.deductCash(playerId, country.getTravelCost(), "Flug nach " + countryName);

        GameCharacter character = characterService.findOrThrow(playerId);
        travel.setDestinationCountry(countryName);
        travel.setArriveAtTurn(character.getCurrentTurn() + country.getTravelMonths());
        travel.setCurrentCountry(null); // in transit
        playerTravelRepository.save(travel);

        return PlayerTravelStatusDto.from(travel);
    }

    @Transactional
    public PlayerTravelStatusDto returnHome(Long playerId) {
        PlayerTravel travel = getOrCreate(playerId);
        if (travel.isTraveling()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Reise erst abwarten bevor du heimkehrst");
        }
        travel.setCurrentCountry(null);
        playerTravelRepository.save(travel);
        return PlayerTravelStatusDto.from(travel);
    }

    public PlayerTravel getOrCreate(Long playerId) {
        return playerTravelRepository.findByPlayerId(playerId)
            .orElseGet(() -> {
                PlayerTravel t = new PlayerTravel(playerId);
                playerTravelRepository.save(t);
                return t;
            });
    }
}
