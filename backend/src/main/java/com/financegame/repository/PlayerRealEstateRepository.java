package com.financegame.repository;

import com.financegame.entity.PlayerRealEstate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlayerRealEstateRepository {

    @PersistenceContext
    private EntityManager em;

    public List<PlayerRealEstate> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT r FROM PlayerRealEstate r JOIN FETCH r.catalog WHERE r.playerId = :playerId",
                PlayerRealEstate.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public Optional<PlayerRealEstate> findById(Long id) {
        return Optional.ofNullable(em.find(PlayerRealEstate.class, id));
    }

    public PlayerRealEstate save(PlayerRealEstate entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public void delete(Long id) {
        PlayerRealEstate entity = em.find(PlayerRealEstate.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }
}
