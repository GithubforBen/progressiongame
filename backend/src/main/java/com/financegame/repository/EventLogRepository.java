package com.financegame.repository;

import com.financegame.entity.EventLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventLogRepository {

    @PersistenceContext
    private EntityManager em;

    public EventLog save(EventLog log) {
        if (log.getId() == null) {
            em.persist(log);
            return log;
        }
        return em.merge(log);
    }

    public List<EventLog> findByPlayerIdOrderByIdDesc(Long playerId, int limit) {
        return em.createQuery(
                "SELECT e FROM EventLog e WHERE e.playerId = :pid ORDER BY e.id DESC",
                EventLog.class)
            .setParameter("pid", playerId)
            .setMaxResults(limit)
            .getResultList();
    }
}
