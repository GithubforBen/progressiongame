package com.financegame.repository;

import com.financegame.entity.PlayerJob;
import com.financegame.entity.PlayerJobId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlayerJobRepository {

    @PersistenceContext
    private EntityManager em;

    public PlayerJob save(PlayerJob playerJob) {
        return em.merge(playerJob);
    }

    public Optional<PlayerJob> findById(PlayerJobId id) {
        return Optional.ofNullable(em.find(PlayerJob.class, id));
    }

    public List<PlayerJob> findActiveByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT pj FROM PlayerJob pj JOIN FETCH pj.job WHERE pj.id.playerId = :playerId AND pj.active = true",
                PlayerJob.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public List<PlayerJob> findAllByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT pj FROM PlayerJob pj JOIN FETCH pj.job WHERE pj.id.playerId = :playerId",
                PlayerJob.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public boolean existsActiveByPlayerAndJob(Long playerId, Long jobId) {
        Long count = em.createQuery(
                "SELECT COUNT(pj) FROM PlayerJob pj WHERE pj.id.playerId = :pid AND pj.id.jobId = :jid AND pj.active = true",
                Long.class)
            .setParameter("pid", playerId)
            .setParameter("jid", jobId)
            .getSingleResult();
        return count > 0;
    }
}
