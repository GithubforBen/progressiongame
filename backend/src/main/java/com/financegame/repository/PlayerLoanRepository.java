package com.financegame.repository;

import com.financegame.entity.PlayerLoan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlayerLoanRepository {

    @PersistenceContext
    private EntityManager em;

    public List<PlayerLoan> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT l FROM PlayerLoan l WHERE l.playerId = :playerId ORDER BY l.id DESC",
                PlayerLoan.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public List<PlayerLoan> findActiveByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT l FROM PlayerLoan l WHERE l.playerId = :playerId AND l.status = 'ACTIVE'",
                PlayerLoan.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public Optional<PlayerLoan> findById(Long id) {
        return Optional.ofNullable(em.find(PlayerLoan.class, id));
    }

    public PlayerLoan save(PlayerLoan entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }
}
