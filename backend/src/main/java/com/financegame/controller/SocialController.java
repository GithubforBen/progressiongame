package com.financegame.controller;

import com.financegame.security.PlayerPrincipal;
import com.financegame.service.SocialService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/network")
    public SocialService.SocialNetworkDto getNetwork(@AuthenticationPrincipal PlayerPrincipal principal) {
        return socialService.getNetwork(principal.id());
    }

    @PostMapping("/persons/{personId}/time")
    public SocialService.ActionResultDto spendTime(@AuthenticationPrincipal PlayerPrincipal principal,
                                                    @PathVariable String personId) {
        return socialService.spendTime(principal.id(), personId);
    }

    @PostMapping("/persons/{personId}/gift")
    public SocialService.ActionResultDto giveGift(@AuthenticationPrincipal PlayerPrincipal principal,
                                                   @PathVariable String personId) {
        return socialService.giveGift(principal.id(), personId);
    }

    @PostMapping("/persons/{personId}/insult")
    public SocialService.ActionResultDto insult(@AuthenticationPrincipal PlayerPrincipal principal,
                                                 @PathVariable String personId) {
        return socialService.insult(principal.id(), personId);
    }

    @PostMapping("/persons/{personId}/rob")
    public SocialService.RobResultDto rob(@AuthenticationPrincipal PlayerPrincipal principal,
                                           @PathVariable String personId) {
        return socialService.rob(principal.id(), personId);
    }
}
