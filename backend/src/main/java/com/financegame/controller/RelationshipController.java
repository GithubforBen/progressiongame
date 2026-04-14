package com.financegame.controller;

import com.financegame.dto.NpcDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.RelationshipService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/npcs")
public class RelationshipController {

    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @GetMapping
    public List<NpcDto> getAll(@AuthenticationPrincipal PlayerPrincipal principal) {
        return relationshipService.getAll(principal.id());
    }

    @PostMapping("/{npcId}/meet")
    public NpcDto meet(@PathVariable Long npcId,
                       @AuthenticationPrincipal PlayerPrincipal principal) {
        return relationshipService.meet(principal.id(), npcId);
    }

    @PostMapping("/{npcId}/interact")
    public NpcDto interact(@PathVariable Long npcId,
                           @AuthenticationPrincipal PlayerPrincipal principal) {
        return relationshipService.interact(principal.id(), npcId);
    }
}
