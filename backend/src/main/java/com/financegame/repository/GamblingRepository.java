package com.financegame.repository;

import com.financegame.entity.GamblingSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GamblingRepository {

    @PersistenceContext
    private EntityManager em;

    public GamblingSession save(GamblingSession session) {
        if (session.getId() == null) {
            em.persist(session);
            return session;
        }
        return em.merge(session);
    }

    public Optional<GamblingSession> findById(Long id) {
        return Optional.ofNullable(em.find(GamblingSession.class, id));
    }
}
