package com.financegame.repository;

import com.financegame.entity.JobApplication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JobApplicationRepository {

    @PersistenceContext
    private EntityManager em;

    public JobApplication save(JobApplication app) {
        if (app.getId() == null) {
            em.persist(app);
            return app;
        }
        return em.merge(app);
    }

    public List<JobApplication> findPendingByPlayerId(Long playerId) {
        return em.createQuery(
                "SELECT a FROM JobApplication a JOIN FETCH a.job WHERE a.playerId = :pid AND a.status = 'PENDING'",
                JobApplication.class)
            .setParameter("pid", playerId)
            .getResultList();
    }

    public List<JobApplication> findByPlayerIdOrderByIdDesc(Long playerId) {
        return em.createQuery(
                "SELECT a FROM JobApplication a JOIN FETCH a.job WHERE a.playerId = :pid ORDER BY a.id DESC",
                JobApplication.class)
            .setParameter("pid", playerId)
            .getResultList();
    }

    public boolean hasPendingForJob(Long playerId, Long jobId) {
        Long count = em.createQuery(
                "SELECT COUNT(a) FROM JobApplication a WHERE a.playerId = :pid AND a.job.id = :jid AND a.status = 'PENDING'",
                Long.class)
            .setParameter("pid", playerId)
            .setParameter("jid", jobId)
            .getSingleResult();
        return count > 0;
    }
}
