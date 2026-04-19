package com.financegame.listener;

import com.financegame.domain.events.LoanDefaultedEvent;
import com.financegame.domain.events.LoanPaidOffEvent;
import com.financegame.domain.events.LoanTakenEvent;
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
 * Verifies that loan lifecycle events are persisted to EventLog by LoanEventListener.
 */
class LoanEventListenerIT extends AbstractListenerIT {

    @Autowired EventLogRepository eventLogRepository;
    @Autowired CharacterRepository characterRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void loanLifecycleEvents_areLoggedToEventLog() {
        Long playerId = 9993L;

        transactionTemplate.executeWithoutResult(status -> {
            GameCharacter character = new GameCharacter();
            character.setPlayerId(playerId);
            characterRepository.save(character);
        });

        LoanEventListener listener = new LoanEventListener(eventLogRepository, characterRepository);

        transactionTemplate.executeWithoutResult(status ->
            listener.onLoanTaken(new LoanTakenEvent(playerId, 1L, new BigDecimal("5000"), "Autokredit"))
        );
        transactionTemplate.executeWithoutResult(status ->
            listener.onLoanPaidOff(new LoanPaidOffEvent(playerId, 1L, "Autokredit"))
        );
        transactionTemplate.executeWithoutResult(status ->
            listener.onLoanDefaulted(new LoanDefaultedEvent(playerId, 2L, "Ratenkredit", new BigDecimal("2000")))
        );

        List<EventLog> logs = eventLogRepository.findByPlayerIdOrderByIdDesc(playerId, 10);
        assertThat(logs).hasSize(3);
        assertThat(logs.stream().map(EventLog::getEventType).toList())
            .containsOnly("LOAN");
        assertThat(logs.stream().map(EventLog::getDescription).toList())
            .anyMatch(d -> d.contains("aufgenommen"))
            .anyMatch(d -> d.contains("abbezahlt"))
            .anyMatch(d -> d.contains("Kreditausfall"));
    }
}
