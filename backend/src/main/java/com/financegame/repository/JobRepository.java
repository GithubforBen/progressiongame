package com.financegame.repository;

import com.financegame.entity.Job;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JobRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Job> findById(Long id) {
        return Optional.ofNullable(em.find(Job.class, id));
    }

    public List<Job> findAllAvailable() {
        return em.createQuery("SELECT j FROM Job j WHERE j.available = true ORDER BY j.salary", Job.class)
            .getResultList();
    }

    public Job save(Job job) {
        if (job.getId() == null) {
            em.persist(job);
            return job;
        }
        return em.merge(job);
    }
}
