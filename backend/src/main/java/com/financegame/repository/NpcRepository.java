package com.financegame.repository;

import com.financegame.entity.Npc;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NpcRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Npc> findAll() {
        return em.createQuery("SELECT n FROM Npc n ORDER BY n.id", Npc.class).getResultList();
    }

    public Optional<Npc> findById(Long id) {
        return Optional.ofNullable(em.find(Npc.class, id));
    }
}
