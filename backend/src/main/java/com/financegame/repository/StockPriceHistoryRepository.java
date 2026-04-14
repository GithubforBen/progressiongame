package com.financegame.repository;

import com.financegame.entity.StockPriceHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StockPriceHistoryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<StockPriceHistory> findByStockIdOrderByTurnAsc(Long stockId) {
        return em.createQuery(
                "SELECT h FROM StockPriceHistory h WHERE h.stockId = :stockId ORDER BY h.turn ASC",
                StockPriceHistory.class)
            .setParameter("stockId", stockId)
            .getResultList();
    }

    public boolean existsByStockIdAndTurn(Long stockId, int turn) {
        Long count = em.createQuery(
                "SELECT COUNT(h) FROM StockPriceHistory h WHERE h.stockId = :sid AND h.turn = :turn",
                Long.class)
            .setParameter("sid", stockId)
            .setParameter("turn", turn)
            .getSingleResult();
        return count > 0;
    }

    public StockPriceHistory save(StockPriceHistory h) {
        if (h.getId() == null) { em.persist(h); return h; }
        return em.merge(h);
    }
}
