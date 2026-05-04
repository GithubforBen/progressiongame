package com.financegame.repository;

import com.financegame.entity.StockPriceHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class StockPriceHistoryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<StockPriceHistory> findByStockIdAndPlayerIdOrderByTurnAsc(Long stockId, Long playerId) {
        return em.createQuery(
                "SELECT h FROM StockPriceHistory h WHERE h.stockId = :stockId AND h.playerId = :playerId ORDER BY h.turn ASC",
                StockPriceHistory.class)
            .setParameter("stockId", stockId)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public boolean existsByStockIdAndPlayerIdAndTurn(Long stockId, Long playerId, int turn) {
        Long count = em.createQuery(
                "SELECT COUNT(h) FROM StockPriceHistory h WHERE h.stockId = :sid AND h.playerId = :pid AND h.turn = :turn",
                Long.class)
            .setParameter("sid", stockId)
            .setParameter("pid", playerId)
            .setParameter("turn", turn)
            .getSingleResult();
        return count > 0;
    }

    public Optional<BigDecimal> findLatestPriceByStockIdAndPlayerId(Long stockId, Long playerId) {
        List<BigDecimal> results = em.createQuery(
                "SELECT h.price FROM StockPriceHistory h WHERE h.stockId = :sid AND h.playerId = :pid ORDER BY h.turn DESC",
                BigDecimal.class)
            .setParameter("sid", stockId)
            .setParameter("pid", playerId)
            .setMaxResults(1)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public StockPriceHistory save(StockPriceHistory h) {
        if (h.getId() == null) { em.persist(h); return h; }
        return em.merge(h);
    }

    public void deleteByStockIdAndPlayerId(Long stockId, Long playerId) {
        em.createQuery(
                "DELETE FROM StockPriceHistory h WHERE h.stockId = :sid AND h.playerId = :pid")
            .setParameter("sid", stockId)
            .setParameter("pid", playerId)
            .executeUpdate();
    }
}
