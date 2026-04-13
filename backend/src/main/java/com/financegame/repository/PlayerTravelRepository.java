package com.financegame.repository;

import com.financegame.entity.PlayerTravel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PlayerTravelRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<PlayerTravel> findByPlayerId(Long playerId) {
        return Optional.ofNullable(em.find(PlayerTravel.class, playerId));
    }

    public PlayerTravel save(PlayerTravel travel) {
        if (em.find(PlayerTravel.class, travel.getPlayerId()) == null) {
            em.persist(travel);
            return travel;
        }
        return em.merge(travel);
    }
}
