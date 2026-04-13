package com.financegame.repository;

import com.financegame.entity.Collectible;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CollectibleRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Collectible> findAll() {
        return em.createQuery("SELECT c FROM Collectible c ORDER BY c.countryRequired, c.rarity", Collectible.class)
            .getResultList();
    }

    public Optional<Collectible> findById(Long id) {
        return Optional.ofNullable(em.find(Collectible.class, id));
    }
}
