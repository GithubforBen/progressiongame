package com.financegame.listener;

import com.financegame.domain.events.CollectiblePurchasedEvent;
import com.financegame.domain.events.CollectionCompletedEvent;
import com.financegame.entity.Collectible;
import com.financegame.entity.Collection;
import com.financegame.entity.EventLog;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerCollectible;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.CollectibleRepository;
import com.financegame.repository.CollectionRepository;
import com.financegame.repository.EventLogRepository;
import com.financegame.repository.PlayerCollectibleRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * After a collectible purchase commits, checks whether that purchase completed
 * a collection. If so, publishes CollectionCompletedEvent and writes a
 * celebratory EventLog entry. This keeps collection-completion logic out of
 * every service that can sell a collectible.
 */
@Component
public class CollectionEventListener {

    private final CollectibleRepository collectibleRepository;
    private final PlayerCollectibleRepository playerCollectibleRepository;
    private final CollectionRepository collectionRepository;
    private final EventLogRepository eventLogRepository;
    private final CharacterRepository characterRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CollectionEventListener(CollectibleRepository collectibleRepository,
                                   PlayerCollectibleRepository playerCollectibleRepository,
                                   CollectionRepository collectionRepository,
                                   EventLogRepository eventLogRepository,
                                   CharacterRepository characterRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.collectibleRepository = collectibleRepository;
        this.playerCollectibleRepository = playerCollectibleRepository;
        this.collectionRepository = collectionRepository;
        this.eventLogRepository = eventLogRepository;
        this.characterRepository = characterRepository;
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCollectiblePurchased(CollectiblePurchasedEvent event) {
        String collectionName = event.collectionName();
        if (collectionName == null || collectionName.isBlank()) return;

        Collection collection = collectionRepository.findByName(collectionName).orElse(null);
        if (collection == null) return;

        List<Collectible> allInCollection = collectibleRepository.findByCollectionName(collectionName);
        if (allInCollection.isEmpty()) return;

        Set<Long> ownedIds = playerCollectibleRepository.findByPlayerId(event.playerId())
            .stream().map(PlayerCollectible::getCollectibleId).collect(Collectors.toSet());

        long ownedCount = allInCollection.stream()
            .filter(c -> ownedIds.contains(c.getId()))
            .count();

        if (ownedCount < collection.getItemCount()) return;

        // Collection is now complete
        GameCharacter character = characterRepository.findByPlayerId(event.playerId()).orElse(null);
        int turn = character != null ? character.getCurrentTurn() : 0;

        eventLogRepository.save(new EventLog(
            event.playerId(),
            "🏆 Sammlung abgeschlossen: \"" + collection.getDisplayName() + "\"! Bonus: "
                + collection.getBonusType() + " +" + collection.getBonusValue(),
            null, "COLLECTION", turn
        ));

        eventPublisher.publishEvent(new CollectionCompletedEvent(
            event.playerId(),
            collection.getName(),
            collection.getDisplayName(),
            collection.getBonusType(),
            collection.getBonusValue()
        ));
    }
}
