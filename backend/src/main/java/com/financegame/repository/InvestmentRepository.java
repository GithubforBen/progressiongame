package com.financegame.repository;

import com.financegame.entity.Investment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InvestmentRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Investment> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT i FROM Investment i WHERE i.playerId = :playerId", Investment.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public List<Investment> findByStockId(Long stockId) {
        return em.createQuery(
                "SELECT i FROM Investment i WHERE i.stockId = :stockId", Investment.class)
            .setParameter("stockId", stockId)
            .getResultList();
    }

    public Optional<Investment> findById(Long id) {
        return Optional.ofNullable(em.find(Investment.class, id));
    }

    public Investment save(Investment investment) {
        if (investment.getId() == null) { em.persist(investment); return investment; }
        return em.merge(investment);
    }

    public void delete(Long id) {
        Investment inv = em.find(Investment.class, id);
        if (inv != null) em.remove(inv);
    }
}
