package com.financegame.controller;

import com.financegame.dto.CharacterDto;
import com.financegame.entity.GameCharacter;
import com.financegame.repository.CharacterRepository;
import com.financegame.security.PlayerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/character")
public class CharacterController {

    private final CharacterRepository characterRepository;

    public CharacterController(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    @GetMapping
    public CharacterDto getCharacter(@AuthenticationPrincipal PlayerPrincipal principal) {
        GameCharacter character = characterRepository.findByPlayerId(principal.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Charakter nicht gefunden"));
        return CharacterDto.from(character);
    }
}
