package com.financegame.controller;

import com.financegame.dto.EducationProgressDto;
import com.financegame.dto.EnrollMainRequest;
import com.financegame.dto.EnrollSideRequest;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.EducationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/education")
public class EducationController {

    private final EducationService educationService;

    public EducationController(EducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping
    public EducationProgressDto getProgress(@AuthenticationPrincipal PlayerPrincipal principal) {
        return educationService.getProgress(principal.id());
    }

    @PostMapping("/main")
    public EducationProgressDto enrollMain(
        @Valid @RequestBody EnrollMainRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return educationService.enrollMain(principal.id(), request);
    }

    @PostMapping("/side")
    public EducationProgressDto enrollSide(
        @Valid @RequestBody EnrollSideRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return educationService.enrollSide(principal.id(), request);
    }
}
