package com.financegame.repository;

import com.financegame.entity.LifestyleItemCatalog;
import com.financegame.entity.PlayerLifestyleItem;
import com.financegame.entity.PlayerLifestyleItemId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LifestyleItemRepository {

    @PersistenceContext
    private EntityManager em;

    public List<LifestyleItemCatalog> findAllCatalog() {
        return em.createQuery("SELECT l FROM LifestyleItemCatalog l ORDER BY l.cost ASC", LifestyleItemCatalog.class)
            .getResultList();
    }

    public Optional<LifestyleItemCatalog> findCatalogById(String id) {
        return Optional.ofNullable(em.find(LifestyleItemCatalog.class, id));
    }

    public List<PlayerLifestyleItem> findByPlayerId(Long playerId) {
        return em.createQuery("SELECT p FROM PlayerLifestyleItem p WHERE p.playerId = :pid", PlayerLifestyleItem.class)
            .setParameter("pid", playerId)
            .getResultList();
    }

    public boolean existsByPlayerIdAndItemId(Long playerId, String itemId) {
        return em.find(PlayerLifestyleItem.class, new PlayerLifestyleItemId(playerId, itemId)) != null;
    }

    public void save(PlayerLifestyleItem item) {
        em.persist(item);
    }
}
