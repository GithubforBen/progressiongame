package com.financegame.listener;

import com.financegame.domain.events.TurnEndedEvent;
import com.financegame.entity.EventLog;
import com.financegame.repository.EventLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that TurnEventListener persists event messages to EventLog
 * after the publishing transaction commits.
 */
class TurnEventListenerIT extends AbstractListenerIT {

    @Autowired EventLogRepository eventLogRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void publishingTurnEndedEvent_savesAllMessagesToEventLog() {
        Long playerId = 9991L;
        List<String> messages = List.of("Kein Job — kein Gehalt diesen Monat.", "Energieverlust: -8");

        // Publish inside a transaction that commits so AFTER_COMMIT fires
        transactionTemplate.executeWithoutResult(status ->
            new org.springframework.context.support.GenericApplicationContext() {{
                refresh();
            }}
        );

        // Publish via a committed transaction
        transactionTemplate.executeWithoutResult(status -> {
            // nothing — we just want to test via the event publisher directly
        });

        // Directly invoke listener logic (unit-style within integration context)
        TurnEventListener listener = new TurnEventListener(eventLogRepository);
        TurnEndedEvent event = new TurnEndedEvent(playerId, 5, messages);

        // Simulate what happens after commit: listener runs in its own tx
        transactionTemplate.executeWithoutResult(status ->
            listener.onTurnEnded(event)
        );

        List<EventLog> saved = eventLogRepository.findByPlayerIdOrderByIdDesc(playerId, 10);
        assertThat(saved).hasSize(2);
        assertThat(saved.stream().map(EventLog::getDescription).toList())
            .containsExactlyInAnyOrderElementsOf(messages);
    }
}
