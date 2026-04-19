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

    public List<Collectible> findByCollectionName(String collectionName) {
        return em.createQuery(
                "SELECT c FROM Collectible c WHERE c.collectionName = :name ORDER BY c.rarity",
                Collectible.class)
            .setParameter("name", collectionName)
            .getResultList();
    }

    public List<Collectible> findByCountryRequired(String country) {
        return em.createQuery(
                "SELECT c FROM Collectible c WHERE c.countryRequired = :country ORDER BY c.rarity",
                Collectible.class)
            .setParameter("country", country)
            .getResultList();
    }
}
