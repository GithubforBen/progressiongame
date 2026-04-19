package com.financegame.listener;

import com.financegame.domain.events.TurnEndedEvent;
import com.financegame.entity.EventLog;
import com.financegame.repository.EventLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persists all turn event messages to EventLog after the turn transaction commits.
 * This removes EventLogRepository from TurnService — the service focuses on
 * game logic only; the listener owns the logging concern.
 */
@Component
public class TurnEventListener {

    private final EventLogRepository eventLogRepository;

    public TurnEventListener(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTurnEnded(TurnEndedEvent event) {
        for (String message : event.eventMessages()) {
            eventLogRepository.save(new EventLog(
                event.playerId(), message, null, "TURN", event.turnNumber()
            ));
        }
    }
}
