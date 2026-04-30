package com.financegame.controller;

import com.financegame.dto.LeaderboardEntryDto;
import com.financegame.dto.PublicCollectionDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.CollectionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    @PersistenceContext
    private EntityManager em;

    private final CollectionService collectionService;

    public LeaderboardController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public List<LeaderboardEntryDto> leaderboard(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(defaultValue = "income") String sort) {

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT p.id, p.username, c.net_worth, c.current_turn, " +
            "       COALESCE(ms.total_income, 0) AS monthly_income, " +
            "       COALESCE(loans.total_debt, 0) AS total_debt " +
            "FROM players p " +
            "JOIN characters c ON c.player_id = p.id " +
            "LEFT JOIN (" +
            "    SELECT DISTINCT ON (player_id) player_id, total_income " +
            "    FROM monthly_snapshots ORDER BY player_id, turn DESC" +
            ") ms ON ms.player_id = p.id " +
            "LEFT JOIN (" +
            "    SELECT player_id, SUM(amount_remaining) AS total_debt " +
            "    FROM player_loans WHERE status = 'ACTIVE' GROUP BY player_id" +
            ") loans ON loans.player_id = p.id"
        ).getResultList();

        // Sort in Java: netWorth sort uses (net_worth - debt), income sort uses monthly income
        Comparator<Object[]> comparator = "netWorth".equals(sort)
            ? Comparator.comparing((Object[] r) -> ((BigDecimal) r[2]).subtract((BigDecimal) r[5]),
                Comparator.reverseOrder())
            : Comparator.comparing(r -> ((BigDecimal) r[4]), Comparator.reverseOrder());
        rows.sort(comparator);

        // Pre-load completed-collection counts for all players in one query
        Map<Long, Integer> completedMap = buildCompletedCollectionsMap();

        List<LeaderboardEntryDto> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Long playerId      = ((Number) row[0]).longValue();
            String username    = (String) row[1];
            BigDecimal netWorth = (BigDecimal) row[2];
            int currentTurn   = ((Number) row[3]).intValue();
            BigDecimal monthlyIncome = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;
            BigDecimal totalDebt     = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;
            BigDecimal adjustedNetWorth = netWorth.subtract(totalDebt);
            int completed = completedMap.getOrDefault(playerId, 0);
            result.add(new LeaderboardEntryDto(
                rank++, playerId, username,
                netWorth, totalDebt, adjustedNetWorth,
                monthlyIncome, completed, currentTurn,
                playerId.equals(principal.id())
            ));
        }
        return result;
    }

    @GetMapping("/player/{playerId}/collections")
    public List<PublicCollectionDto> playerCollections(@PathVariable Long playerId) {
        return collectionService.getPublicCollections(playerId);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> buildCompletedCollectionsMap() {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT pc.player_id, COUNT(*) AS completed_count " +
            "FROM (" +
            "    SELECT cl.collection_name, pc2.player_id, " +
            "           COUNT(pc2.collectible_id) AS owned_count, " +
            "           MAX(col.item_count) AS required_count " +
            "    FROM player_collectibles pc2 " +
            "    JOIN collectibles cl ON cl.id = pc2.collectible_id " +
            "    JOIN collections col ON col.name = cl.collection_name " +
            "    GROUP BY cl.collection_name, pc2.player_id " +
            "    HAVING COUNT(pc2.collectible_id) >= MAX(col.item_count)" +
            ") pc " +
            "GROUP BY pc.player_id"
        ).getResultList();

        return rows.stream().collect(Collectors.toMap(
            r -> ((Number) r[0]).longValue(),
            r -> ((Number) r[1]).intValue()
        ));
    }
}
