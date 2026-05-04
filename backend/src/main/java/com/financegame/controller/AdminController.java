package com.financegame.controller;

import com.financegame.dto.AdminCharacterEditRequest;
import com.financegame.dto.AdminPlayerDetailDto;
import com.financegame.dto.AdminPlayerDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/me")
    public Map<String, Boolean> me(@AuthenticationPrincipal PlayerPrincipal principal) {
        return Map.of("isAdmin", adminService.isAdmin(principal));
    }

    @GetMapping("/players")
    public List<AdminPlayerDto> listPlayers(@AuthenticationPrincipal PlayerPrincipal principal) {
        adminService.requireAdmin(principal);
        return adminService.listAllPlayers();
    }

    @PatchMapping("/players/{playerId}/character")
    public AdminPlayerDto editCharacter(
        @AuthenticationPrincipal PlayerPrincipal principal,
        @PathVariable Long playerId,
        @RequestBody AdminCharacterEditRequest request
    ) {
        adminService.requireAdmin(principal);
        return adminService.editCharacter(playerId, request);
    }

    @GetMapping("/players/{playerId}/details")
    public AdminPlayerDetailDto getPlayerDetails(
        @AuthenticationPrincipal PlayerPrincipal principal,
        @PathVariable Long playerId
    ) {
        adminService.requireAdmin(principal);
        return adminService.getPlayerDetails(playerId);
    }

    @DeleteMapping("/players/{playerId}/real-estate/{realEstateId}")
    public ResponseEntity<Void> deleteRealEstate(
        @AuthenticationPrincipal PlayerPrincipal principal,
        @PathVariable Long playerId,
        @PathVariable Long realEstateId
    ) {
        adminService.requireAdmin(principal);
        adminService.deleteRealEstate(playerId, realEstateId);
        return ResponseEntity.noContent().build();
    }
}
