package com.financegame.repository;

import com.financegame.entity.PlayerRelationship;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlayerRelationshipRepository {

    @PersistenceContext
    private EntityManager em;

    public PlayerRelationship save(PlayerRelationship rel) {
        if (rel.getId() == null) { em.persist(rel); return rel; }
        return em.merge(rel);
    }

    public List<PlayerRelationship> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT r FROM PlayerRelationship r WHERE r.playerId = :pid",
                PlayerRelationship.class)
            .setParameter("pid", playerId)
            .getResultList();
    }

    public Optional<PlayerRelationship> findByPlayerIdAndNpcId(Long playerId, Long npcId) {
        return em.createQuery(
                "SELECT r FROM PlayerRelationship r WHERE r.playerId = :pid AND r.npcId = :nid",
                PlayerRelationship.class)
            .setParameter("pid", playerId)
            .setParameter("nid", npcId)
            .getResultStream()
            .findFirst();
    }
}
