package com.financegame.repository;

import com.financegame.entity.MonthlyExpense;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MonthlyExpenseRepository {

    @PersistenceContext
    private EntityManager em;

    public MonthlyExpense save(MonthlyExpense expense) {
        if (expense.getId() == null) {
            em.persist(expense);
            return expense;
        }
        return em.merge(expense);
    }

    public List<MonthlyExpense> findByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT e FROM MonthlyExpense e WHERE e.playerId = :playerId", MonthlyExpense.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public List<MonthlyExpense> findActiveByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT e FROM MonthlyExpense e WHERE e.playerId = :playerId AND e.active = true",
                MonthlyExpense.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public void delete(Long expenseId) {
        MonthlyExpense expense = em.find(MonthlyExpense.class, expenseId);
        if (expense != null) {
            em.remove(expense);
        }
    }
}
