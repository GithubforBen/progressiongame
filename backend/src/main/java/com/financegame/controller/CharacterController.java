package com.financegame.controller;

import com.financegame.dto.CharacterDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.CharacterService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/character")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping
    public CharacterDto getCharacter(@AuthenticationPrincipal PlayerPrincipal principal) {
        return characterService.getCharacter(principal.id());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetCharacter(@AuthenticationPrincipal PlayerPrincipal principal) {
        characterService.resetCharacter(principal.id());
    }
}
