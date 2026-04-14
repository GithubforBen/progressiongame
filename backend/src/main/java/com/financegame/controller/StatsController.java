package com.financegame.controller;

import com.financegame.dto.SnapshotDto;
import com.financegame.repository.MonthlySnapshotRepository;
import com.financegame.security.PlayerPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final MonthlySnapshotRepository monthlySnapshotRepository;

    public StatsController(MonthlySnapshotRepository monthlySnapshotRepository) {
        this.monthlySnapshotRepository = monthlySnapshotRepository;
    }

    @GetMapping("/snapshots")
    public List<SnapshotDto> snapshots(@AuthenticationPrincipal PlayerPrincipal principal) {
        return monthlySnapshotRepository
            .findByPlayerIdOrderByTurn(principal.id())
            .stream()
            .map(SnapshotDto::from)
            .toList();
    }
}
