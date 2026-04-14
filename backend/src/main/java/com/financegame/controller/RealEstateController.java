package com.financegame.controller;

import com.financegame.dto.ChangeModeRequest;
import com.financegame.dto.PlayerRealEstateDto;
import com.financegame.dto.RealEstateDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.RealEstateService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/real-estate")
public class RealEstateController {

    private final RealEstateService realEstateService;

    public RealEstateController(RealEstateService realEstateService) {
        this.realEstateService = realEstateService;
    }

    @GetMapping
    public List<RealEstateDto> getCatalog(@AuthenticationPrincipal PlayerPrincipal principal) {
        return realEstateService.getCatalog(principal.id());
    }

    @GetMapping("/my")
    public List<PlayerRealEstateDto> getMyProperties(@AuthenticationPrincipal PlayerPrincipal principal) {
        return realEstateService.getMyProperties(principal.id());
    }

    @PostMapping("/{id}/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerRealEstateDto buy(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return realEstateService.buy(principal.id(), id);
    }

    @PatchMapping("/{id}/mode")
    public PlayerRealEstateDto changeMode(
        @PathVariable Long id,
        @RequestBody ChangeModeRequest req,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return realEstateService.changeMode(principal.id(), id, req);
    }
}
