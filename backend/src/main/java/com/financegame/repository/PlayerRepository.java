package com.financegame.repository;

import com.financegame.entity.Player;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PlayerRepository {

    @PersistenceContext
    private EntityManager em;

    public Player save(Player player) {
        if (player.getId() == null) {
            em.persist(player);
            return player;
        }
        return em.merge(player);
    }

    public Optional<Player> findById(Long id) {
        return Optional.ofNullable(em.find(Player.class, id));
    }

    public Optional<Player> findByUsername(String username) {
        try {
            Player player = em.createQuery(
                    "SELECT p FROM Player p WHERE p.username = :username", Player.class)
                .setParameter("username", username)
                .getSingleResult();
            return Optional.of(player);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean existsByUsername(String username) {
        Long count = em.createQuery(
                "SELECT COUNT(p) FROM Player p WHERE p.username = :username", Long.class)
            .setParameter("username", username)
            .getSingleResult();
        return count > 0;
    }
}
