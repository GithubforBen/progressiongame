package com.financegame.repository;

import com.financegame.entity.PlayerDelistedStock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PlayerDelistedStockRepository {

    @PersistenceContext
    private EntityManager em;

    public boolean isDelisted(Long playerId, Long stockId) {
        Long count = em.createQuery(
                "SELECT COUNT(d) FROM PlayerDelistedStock d WHERE d.playerId = :pid AND d.stockId = :sid",
                Long.class)
            .setParameter("pid", playerId)
            .setParameter("sid", stockId)
            .getSingleResult();
        return count > 0;
    }

    public Set<Long> findDelistedStockIds(Long playerId) {
        return em.createQuery(
                "SELECT d.stockId FROM PlayerDelistedStock d WHERE d.playerId = :pid",
                Long.class)
            .setParameter("pid", playerId)
            .getResultStream()
            .collect(Collectors.toSet());
    }

    public List<PlayerDelistedStock> findAllForPlayer(Long playerId) {
        return em.createQuery(
                "SELECT d FROM PlayerDelistedStock d WHERE d.playerId = :pid",
                PlayerDelistedStock.class)
            .setParameter("pid", playerId)
            .getResultList();
    }

    public PlayerDelistedStock save(PlayerDelistedStock d) {
        if (d.getId() == null) { em.persist(d); return d; }
        return em.merge(d);
    }

    public void delete(Long playerId, Long stockId) {
        em.createQuery(
                "DELETE FROM PlayerDelistedStock d WHERE d.playerId = :pid AND d.stockId = :sid")
            .setParameter("pid", playerId)
            .setParameter("sid", stockId)
            .executeUpdate();
    }
}
