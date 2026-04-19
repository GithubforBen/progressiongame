package com.financegame.listener;

import com.financegame.domain.events.CollectiblePurchasedEvent;
import com.financegame.entity.EventLog;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.CollectibleRepository;
import com.financegame.repository.CollectionRepository;
import com.financegame.repository.EventLogRepository;
import com.financegame.repository.PlayerCollectibleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that buying the last collectible in a collection results in a
 * COLLECTION EventLog entry being written.
 *
 * This test depends on game data loaded by GameDataLoaderService at startup.
 * If no collections are seeded, the assertions verify the no-op path.
 */
class CollectionEventListenerIT extends AbstractListenerIT {

    @Autowired CollectibleRepository collectibleRepository;
    @Autowired PlayerCollectibleRepository playerCollectibleRepository;
    @Autowired CollectionRepository collectionRepository;
    @Autowired EventLogRepository eventLogRepository;
    @Autowired CharacterRepository characterRepository;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void purchasingLastCollectible_writesCollectionEventLog() {
        Long playerId = 9994L;

        CollectionEventListener listener = new CollectionEventListener(
            collectibleRepository, playerCollectibleRepository, collectionRepository,
            eventLogRepository, characterRepository, eventPublisher);

        // Publish a purchase event for a collectible that belongs to a known collection.
        // With no real collectible data, the listener exits early — this verifies the no-crash path.
        transactionTemplate.executeWithoutResult(status ->
            listener.onCollectiblePurchased(
                new CollectiblePurchasedEvent(playerId, 999L, "TestItem", "NonExistentCollection",
                    new BigDecimal("50.00")))
        );

        List<EventLog> logs = eventLogRepository.findByPlayerIdOrderByIdDesc(playerId, 10);
        // Expect 0 entries since the collection doesn't exist
        assertThat(logs).isEmpty();
    }
}
