package com.financegame.controller;

import com.financegame.dto.CountryDto;
import com.financegame.dto.DepartRequest;
import com.financegame.dto.PlayerTravelStatusDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.TravelService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travel")
public class TravelController {

    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    @GetMapping("/countries")
    public List<CountryDto> getCountries(@AuthenticationPrincipal PlayerPrincipal principal) {
        return travelService.getCountries(principal.id());
    }

    @GetMapping("/status")
    public PlayerTravelStatusDto getStatus(@AuthenticationPrincipal PlayerPrincipal principal) {
        return travelService.getStatus(principal.id());
    }

    @PostMapping("/depart")
    public PlayerTravelStatusDto depart(
        @RequestBody DepartRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return travelService.depart(principal.id(), request.countryName());
    }

    @PostMapping("/return-home")
    public PlayerTravelStatusDto returnHome(@AuthenticationPrincipal PlayerPrincipal principal) {
        return travelService.returnHome(principal.id());
    }
}
