package com.financegame.repository;

import com.financegame.entity.Stock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StockRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Stock> findAll() {
        return em.createQuery("SELECT s FROM Stock s ORDER BY s.name", Stock.class)
            .getResultList();
    }

    public Optional<Stock> findByTicker(String ticker) {
        return em.createQuery("SELECT s FROM Stock s WHERE s.ticker = :ticker", Stock.class)
            .setParameter("ticker", ticker)
            .getResultStream()
            .findFirst();
    }

    public Stock save(Stock stock) {
        if (stock.getId() == null) { em.persist(stock); return stock; }
        return em.merge(stock);
    }
}
