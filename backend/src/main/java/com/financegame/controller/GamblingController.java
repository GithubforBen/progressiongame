package com.financegame.controller;

import com.financegame.dto.BlackjackStateDto;
import com.financegame.dto.GamblingBetRequest;
import com.financegame.dto.PokerResultDto;
import com.financegame.dto.RouletteRequest;
import com.financegame.dto.RouletteResultDto;
import com.financegame.dto.SlotResultDto;
import com.financegame.dto.TexasHoldemActionRequest;
import com.financegame.dto.TexasHoldemStateDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.GamblingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gambling")
public class GamblingController {

    private final GamblingService gamblingService;

    public GamblingController(GamblingService gamblingService) {
        this.gamblingService = gamblingService;
    }

    // --- Slots ---

    @PostMapping("/slots")
    public SlotResultDto playSlots(
        @Valid @RequestBody GamblingBetRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return gamblingService.playSlots(principal.id(), request.bet());
    }

    // --- Blackjack ---

    @PostMapping("/blackjack/start")
    public BlackjackStateDto startBlackjack(
        @Valid @RequestBody GamblingBetRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return gamblingService.startBlackjack(principal.id(), request.bet());
    }

    @PostMapping("/blackjack/{id}/hit")
    public BlackjackStateDto hit(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return gamblingService.hitBlackjack(principal.id(), id);
    }

    @PostMapping("/blackjack/{id}/stand")
    public BlackjackStateDto stand(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return gamblingService.standBlackjack(principal.id(), id);
    }

    // --- Poker ---

    @PostMapping("/poker")
    public PokerResultDto playPoker(
        @Valid @RequestBody GamblingBetRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return gamblingService.playPoker(principal.id(), request.bet());
    }

    // --- Texas Hold'em ---

    @PostMapping("/texasholdem/start")
    public TexasHoldemStateDto startTexasHoldem(
        @Valid @RequestBody GamblingBetRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return gamblingService.startTexasHoldem(principal.id(), request.bet());
    }

    @PostMapping("/texasholdem/{id}/action")
    public TexasHoldemStateDto actTexasHoldem(
        @PathVariable Long id,
        @RequestBody TexasHoldemActionRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return gamblingService.actTexasHoldem(principal.id(), id, request.action(), request.amount());
    }

    // --- Roulette ---

    @PostMapping("/roulette")
    public RouletteResultDto playRoulette(
        @Valid @RequestBody RouletteRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return gamblingService.playRoulette(principal.id(), request);
    }
}
