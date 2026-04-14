package com.financegame.repository;

import com.financegame.entity.RealEstateCatalog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RealEstateCatalogRepository {

    @PersistenceContext
    private EntityManager em;

    public List<RealEstateCatalog> findAll() {
        return em.createQuery("SELECT r FROM RealEstateCatalog r ORDER BY r.purchasePrice", RealEstateCatalog.class)
            .getResultList();
    }

    public Optional<RealEstateCatalog> findById(Long id) {
        return Optional.ofNullable(em.find(RealEstateCatalog.class, id));
    }

    public RealEstateCatalog save(RealEstateCatalog entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }
}
