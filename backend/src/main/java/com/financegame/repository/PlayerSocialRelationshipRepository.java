package com.financegame.repository;

import com.financegame.entity.PlayerSocialRelationship;
import com.financegame.entity.PlayerSocialRelationshipId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlayerSocialRelationshipRepository {

    @PersistenceContext
    private EntityManager em;

    public List<PlayerSocialRelationship> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT r FROM PlayerSocialRelationship r WHERE r.playerId = :pid",
                PlayerSocialRelationship.class)
            .setParameter("pid", playerId)
            .getResultList();
    }

    public Optional<PlayerSocialRelationship> findById(Long playerId, String personId) {
        return Optional.ofNullable(
            em.find(PlayerSocialRelationship.class, new PlayerSocialRelationshipId(playerId, personId))
        );
    }

    public PlayerSocialRelationship save(PlayerSocialRelationship r) {
        if (em.find(PlayerSocialRelationship.class,
                new PlayerSocialRelationshipId(r.getPlayerId(), r.getPersonId())) == null) {
            em.persist(r);
        } else {
            return em.merge(r);
        }
        return r;
    }
}
