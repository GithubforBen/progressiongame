package com.financegame.repository;

import com.financegame.entity.NeedsItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NeedsItemRepository {

    @PersistenceContext
    private EntityManager em;

    public NeedsItem save(NeedsItem item) {
        return em.merge(item);
    }

    public Optional<NeedsItem> findById(String id) {
        return Optional.ofNullable(em.find(NeedsItem.class, id));
    }

    public List<NeedsItem> findAll() {
        return em.createQuery("SELECT n FROM NeedsItem n ORDER BY n.price ASC", NeedsItem.class)
            .getResultList();
    }
}
