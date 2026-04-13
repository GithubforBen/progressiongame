package com.financegame.repository;

import com.financegame.entity.GameCharacter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CharacterRepository {

    @PersistenceContext
    private EntityManager em;

    public GameCharacter save(GameCharacter character) {
        if (character.getId() == null) {
            em.persist(character);
            return character;
        }
        return em.merge(character);
    }

    public Optional<GameCharacter> findByPlayerId(Long playerId) {
        try {
            GameCharacter c = em.createQuery(
                    "SELECT c FROM GameCharacter c WHERE c.playerId = :playerId", GameCharacter.class)
                .setParameter("playerId", playerId)
                .getSingleResult();
            return Optional.of(c);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
