package com.financegame.repository;

import com.financegame.entity.EducationProgress;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EducationProgressRepository {

    @PersistenceContext
    private EntityManager em;

    public EducationProgress save(EducationProgress ep) {
        if (ep.getId() == null) {
            em.persist(ep);
            return ep;
        }
        return em.merge(ep);
    }

    public Optional<EducationProgress> findByPlayerId(Long playerId) {
        try {
            EducationProgress ep = em.createQuery(
                    "SELECT e FROM EducationProgress e WHERE e.playerId = :playerId",
                    EducationProgress.class)
                .setParameter("playerId", playerId)
                .getSingleResult();
            return Optional.of(ep);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
