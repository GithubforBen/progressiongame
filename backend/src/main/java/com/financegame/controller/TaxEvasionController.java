package com.financegame.controller;

import com.financegame.dto.CharacterDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.TaxEvasionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tax-evasion")
public class TaxEvasionController {

    private final TaxEvasionService taxEvasionService;

    public TaxEvasionController(TaxEvasionService taxEvasionService) {
        this.taxEvasionService = taxEvasionService;
    }

    @GetMapping("/status")
    public TaxEvasionService.StatusDto getStatus(@AuthenticationPrincipal PlayerPrincipal principal) {
        return taxEvasionService.getStatus(principal.id());
    }

    @PostMapping("/toggle")
    public CharacterDto toggle(@AuthenticationPrincipal PlayerPrincipal principal) {
        return taxEvasionService.toggle(principal.id());
    }

    @PostMapping("/resolve-caught")
    public CharacterDto resolveCaught(
        @AuthenticationPrincipal PlayerPrincipal principal,
        @RequestBody ResolveRequest body
    ) {
        return taxEvasionService.resolveCaught(principal.id(), body.choice());
    }

    public record ResolveRequest(String choice) {}
}
