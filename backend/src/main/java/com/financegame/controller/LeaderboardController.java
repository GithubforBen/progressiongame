package com.financegame.controller;

import com.financegame.dto.LeaderboardEntryDto;
import com.financegame.security.PlayerPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    public List<LeaderboardEntryDto> leaderboard(@AuthenticationPrincipal PlayerPrincipal principal) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT player_id, username, net_worth, current_turn FROM leaderboard")
            .getResultList();

        List<LeaderboardEntryDto> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Long playerId = ((Number) row[0]).longValue();
            String username = (String) row[1];
            BigDecimal netWorth = (BigDecimal) row[2];
            int currentTurn = ((Number) row[3]).intValue();
            result.add(new LeaderboardEntryDto(rank++, playerId, username, netWorth,
                                               currentTurn, playerId.equals(principal.id())));
        }
        return result;
    }
}
