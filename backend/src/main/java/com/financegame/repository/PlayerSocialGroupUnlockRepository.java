package com.financegame.repository;

import com.financegame.entity.PlayerSocialGroupUnlock;
import com.financegame.entity.PlayerSocialGroupUnlockId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PlayerSocialGroupUnlockRepository {

    @PersistenceContext
    private EntityManager em;

    public Set<String> findUnlockedGroupIds(Long playerId) {
        return em.createQuery(
                "SELECT u.groupId FROM PlayerSocialGroupUnlock u WHERE u.playerId = :pid",
                String.class)
            .setParameter("pid", playerId)
            .getResultStream()
            .collect(Collectors.toSet());
    }

    public List<PlayerSocialGroupUnlock> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT u FROM PlayerSocialGroupUnlock u WHERE u.playerId = :pid",
                PlayerSocialGroupUnlock.class)
            .setParameter("pid", playerId)
            .getResultList();
    }

    public boolean exists(Long playerId, String groupId) {
        return em.find(PlayerSocialGroupUnlock.class,
            new PlayerSocialGroupUnlockId(playerId, groupId)) != null;
    }

    public PlayerSocialGroupUnlock save(PlayerSocialGroupUnlock u) {
        if (!exists(u.getPlayerId(), u.getGroupId())) {
            em.persist(u);
        } else {
            return em.merge(u);
        }
        return u;
    }
}
