package com.financegame.controller;

import com.financegame.dto.CharacterDto;
import com.financegame.entity.GameCharacter;
import com.financegame.repository.CharacterRepository;
import com.financegame.security.PlayerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/turn")
public class TurnController {

    private final CharacterRepository characterRepository;

    public TurnController(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    /**
     * Stub endpoint — full turn engine implemented in Step 4.
     * Increments current_turn and returns updated character.
     */
    @PostMapping("/end")
    public Map<String, CharacterDto> endTurn(@AuthenticationPrincipal PlayerPrincipal principal) {
        GameCharacter character = characterRepository.findByPlayerId(principal.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Charakter nicht gefunden"));

        character.setCurrentTurn(character.getCurrentTurn() + 1);
        characterRepository.save(character);

        return Map.of("character", CharacterDto.from(character));
    }
}
