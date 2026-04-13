package com.financegame.repository;

import com.financegame.entity.PlayerCollectible;
import com.financegame.entity.PlayerCollectibleId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PlayerCollectibleRepository {

    @PersistenceContext
    private EntityManager em;

    public List<PlayerCollectible> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT pc FROM PlayerCollectible pc WHERE pc.playerId = :playerId",
                PlayerCollectible.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public boolean existsById(Long playerId, Long collectibleId) {
        return em.find(PlayerCollectible.class, new PlayerCollectibleId(playerId, collectibleId)) != null;
    }

    public PlayerCollectible save(PlayerCollectible pc) {
        em.persist(pc);
        return pc;
    }
}
