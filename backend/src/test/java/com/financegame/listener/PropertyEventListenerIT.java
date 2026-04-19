package com.financegame.listener;

import com.financegame.domain.events.PropertyModeChangedEvent;
import com.financegame.domain.events.PropertyPurchasedEvent;
import com.financegame.entity.EventLog;
import com.financegame.entity.GameCharacter;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.EventLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that PropertyEventListener logs real-estate purchases and mode changes.
 */
class PropertyEventListenerIT extends AbstractListenerIT {

    @Autowired EventLogRepository eventLogRepository;
    @Autowired CharacterRepository characterRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void propertyEvents_areLoggedToEventLog() {
        Long playerId = 9996L;

        transactionTemplate.executeWithoutResult(status -> {
            GameCharacter character = new GameCharacter();
            character.setPlayerId(playerId);
            characterRepository.save(character);
        });

        PropertyEventListener listener = new PropertyEventListener(eventLogRepository, characterRepository);

        transactionTemplate.executeWithoutResult(status ->
            listener.onPropertyPurchased(
                new PropertyPurchasedEvent(playerId, 1L, "Berliner Altbau", new BigDecimal("180000")))
        );
        transactionTemplate.executeWithoutResult(status ->
            listener.onPropertyModeChanged(new PropertyModeChangedEvent(playerId, 1L, "RENTED_OUT"))
        );

        List<EventLog> logs = eventLogRepository.findByPlayerIdOrderByIdDesc(playerId, 10);
        assertThat(logs).hasSize(2);
        assertThat(logs.stream().map(EventLog::getEventType).toList())
            .containsOnly("REAL_ESTATE");
        assertThat(logs.stream().map(EventLog::getDescription).toList())
            .anyMatch(d -> d.contains("Berliner Altbau"))
            .anyMatch(d -> d.contains("vermietet"));
    }
}
