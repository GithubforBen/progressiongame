package com.financegame.listener;

import com.financegame.domain.events.StockPurchasedEvent;
import com.financegame.domain.events.StockSoldEvent;
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
 * Verifies that InvestmentEventListener logs stock buy/sell trades to EventLog.
 */
class InvestmentEventListenerIT extends AbstractListenerIT {

    @Autowired EventLogRepository eventLogRepository;
    @Autowired CharacterRepository characterRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void stockTradeEvents_areLoggedToEventLog() {
        Long playerId = 9997L;

        transactionTemplate.executeWithoutResult(status -> {
            GameCharacter character = new GameCharacter();
            character.setPlayerId(playerId);
            characterRepository.save(character);
        });

        InvestmentEventListener listener = new InvestmentEventListener(eventLogRepository, characterRepository);

        transactionTemplate.executeWithoutResult(status ->
            listener.onStockPurchased(new StockPurchasedEvent(
                playerId, "AAPL", new BigDecimal("10"), new BigDecimal("1500")))
        );
        transactionTemplate.executeWithoutResult(status ->
            listener.onStockSold(new StockSoldEvent(
                playerId, "AAPL", new BigDecimal("1800"), new BigDecimal("300")))
        );

        List<EventLog> logs = eventLogRepository.findByPlayerIdOrderByIdDesc(playerId, 10);
        assertThat(logs).hasSize(2);
        assertThat(logs.stream().map(EventLog::getEventType).toList())
            .containsOnly("INVESTMENT");
        assertThat(logs.stream().map(EventLog::getDescription).toList())
            .anyMatch(d -> d.contains("gekauft") && d.contains("AAPL"))
            .anyMatch(d -> d.contains("verkauft") && d.contains("AAPL"));
    }
}
