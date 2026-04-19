package com.financegame.listener;

import com.financegame.domain.events.PlayerRegisteredEvent;
import com.financegame.entity.EventLog;
import com.financegame.repository.EventLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Writes a welcome EventLog entry after a new player's registration transaction commits.
 * Runs asynchronously so registration response is not delayed by this non-critical write.
 */
@Component
public class PlayerRegistrationListener {

    private final EventLogRepository eventLogRepository;

    public PlayerRegistrationListener(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPlayerRegistered(PlayerRegisteredEvent event) {
        eventLogRepository.save(new EventLog(
            event.playerId(),
            "🎮 Willkommen bei FinanzLeben, " + event.username() + "! Dein Abenteuer beginnt jetzt.",
            null, "SYSTEM", 1
        ));
    }
}
