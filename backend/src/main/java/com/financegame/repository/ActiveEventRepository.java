package com.financegame.repository;

import com.financegame.entity.ActiveEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ActiveEventRepository {

    @PersistenceContext
    private EntityManager em;

    public List<ActiveEvent> findActiveForPlayer(Long playerId, int currentTurn) {
        return em.createQuery(
                "SELECT e FROM ActiveEvent e WHERE e.playerId = :playerId AND e.expiresAtTurn >= :turn",
                ActiveEvent.class)
            .setParameter("playerId", playerId)
            .setParameter("turn", currentTurn)
            .getResultList();
    }

    public void deleteExpiredForPlayer(Long playerId, int currentTurn) {
        em.createQuery(
                "DELETE FROM ActiveEvent e WHERE e.playerId = :playerId AND e.expiresAtTurn < :turn")
            .setParameter("playerId", playerId)
            .setParameter("turn", currentTurn)
            .executeUpdate();
    }

    public ActiveEvent save(ActiveEvent event) {
        if (event.getId() == null) { em.persist(event); return event; }
        return em.merge(event);
    }
}
