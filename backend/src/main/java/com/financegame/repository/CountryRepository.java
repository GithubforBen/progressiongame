package com.financegame.repository;

import com.financegame.entity.Country;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CountryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Country> findAll() {
        return em.createQuery("SELECT c FROM Country c ORDER BY c.travelCost", Country.class)
            .getResultList();
    }

    public Optional<Country> findByName(String name) {
        return em.createQuery("SELECT c FROM Country c WHERE c.name = :name", Country.class)
            .setParameter("name", name)
            .getResultStream().findFirst();
    }
}
