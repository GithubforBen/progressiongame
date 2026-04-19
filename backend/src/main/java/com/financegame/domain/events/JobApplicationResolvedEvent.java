package com.financegame.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired by TurnService when a pending job application is evaluated (accepted or rejected).
 * Allows other modules to react — e.g. log the outcome, award a bonus, or trigger onboarding.
 */
public record JobApplicationResolvedEvent(
    Long playerId,
    Long jobId,
    String jobName,
    boolean accepted,
    BigDecimal salary,
    Instant occurredAt
) implements DomainEvent {
    public JobApplicationResolvedEvent(Long playerId, Long jobId, String jobName,
                                       boolean accepted, BigDecimal salary) {
        this(playerId, jobId, jobName, accepted, salary, Instant.now());
    }
}
