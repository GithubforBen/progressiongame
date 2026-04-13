package com.financegame.repository;

import com.financegame.entity.MonthlySnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MonthlySnapshotRepository {

    @PersistenceContext
    private EntityManager em;

    public MonthlySnapshot save(MonthlySnapshot snapshot) {
        if (snapshot.getId() == null) {
            em.persist(snapshot);
            return snapshot;
        }
        return em.merge(snapshot);
    }

    public List<MonthlySnapshot> findByPlayerIdOrderByTurn(Long playerId) {
        return em.createQuery(
                "SELECT s FROM MonthlySnapshot s WHERE s.playerId = :pid ORDER BY s.turn ASC",
                MonthlySnapshot.class)
            .setParameter("pid", playerId)
            .getResultList();
    }
}
