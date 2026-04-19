package com.financegame.listener;

import com.financegame.domain.events.PlayerRegisteredEvent;
import com.financegame.entity.EventLog;
import com.financegame.repository.EventLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifies that PlayerRegistrationListener writes a welcome EventLog entry
 * asynchronously after registration.
 */
class PlayerRegistrationListenerIT extends AbstractListenerIT {

    @Autowired EventLogRepository eventLogRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void playerRegistration_writesWelcomeEventLog() throws InterruptedException {
        Long playerId = 9995L;

        PlayerRegistrationListener listener = new PlayerRegistrationListener(eventLogRepository);

        // Async listener: run in its own transaction and wait for completion
        transactionTemplate.executeWithoutResult(status ->
            listener.onPlayerRegistered(new PlayerRegisteredEvent(playerId, "TestSpieler"))
        );

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EventLog> logs = eventLogRepository.findByPlayerIdOrderByIdDesc(playerId, 5);
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getDescription()).contains("Willkommen");
            assertThat(logs.get(0).getDescription()).contains("TestSpieler");
            assertThat(logs.get(0).getEventType()).isEqualTo("SYSTEM");
        });
    }
}
