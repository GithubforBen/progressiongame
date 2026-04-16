package com.financegame.repository;

import com.financegame.entity.Collection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CollectionRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Collection> findAll() {
        return em.createQuery("SELECT c FROM Collection c ORDER BY c.name", Collection.class)
            .getResultList();
    }

    public Optional<Collection> findByName(String name) {
        var list = em.createQuery("SELECT c FROM Collection c WHERE c.name = :name", Collection.class)
            .setParameter("name", name)
            .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
