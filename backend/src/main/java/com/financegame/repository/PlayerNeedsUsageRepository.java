package com.financegame.repository;

import com.financegame.entity.PlayerNeedsUsage;
import com.financegame.entity.PlayerNeedsUsageId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlayerNeedsUsageRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<PlayerNeedsUsage> findByPlayerAndItem(Long playerId, String itemId) {
        return Optional.ofNullable(em.find(PlayerNeedsUsage.class, new PlayerNeedsUsageId(playerId, itemId)));
    }

    public List<PlayerNeedsUsage> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT u FROM PlayerNeedsUsage u WHERE u.id.playerId = :pid",
                PlayerNeedsUsage.class)
            .setParameter("pid", playerId)
            .getResultList();
    }

    public PlayerNeedsUsage save(PlayerNeedsUsage usage) {
        if (em.find(PlayerNeedsUsage.class, usage.getId()) == null) {
            em.persist(usage);
            return usage;
        }
        return em.merge(usage);
    }
}
