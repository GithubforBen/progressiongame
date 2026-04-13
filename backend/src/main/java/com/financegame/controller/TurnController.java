package com.financegame.controller;

import com.financegame.dto.CharacterDto;
import com.financegame.entity.GameCharacter;
import com.financegame.repository.CharacterRepository;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.CharacterService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/turn")
public class TurnController {

    private final CharacterService characterService;
    private final CharacterRepository characterRepository;

    public TurnController(CharacterService characterService,
                          CharacterRepository characterRepository) {
        this.characterService = characterService;
        this.characterRepository = characterRepository;
    }

    /**
     * Advance one month. Full turn engine implemented in Step 4.
     * Currently: apply needs decay, increment turn counter.
     */
    @PostMapping("/end")
    @Transactional
    public Map<String, CharacterDto> endTurn(@AuthenticationPrincipal PlayerPrincipal principal) {
        // Apply needs decay for this month
        GameCharacter character = characterService.applyNeedsDecay(principal.id());

        // Advance turn counter
        character.setCurrentTurn(character.getCurrentTurn() + 1);
        character = characterRepository.save(character);

        // Sync net worth
        characterService.recalculateNetWorth(principal.id());

        return Map.of("character", CharacterDto.from(character));
    }
}
