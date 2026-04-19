package com.financegame.listener;

import com.financegame.domain.events.TravelArrivedEvent;
import com.financegame.entity.ActiveEvent;
import com.financegame.entity.Collectible;
import com.financegame.entity.GameCharacter;
import com.financegame.repository.ActiveEventRepository;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.CollectibleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Random;

/**
 * When a player arrives in a new country, creates a COLLECTIBLE_SALE ActiveEvent
 * for a collectible available in that country. This decouples the "arrival reward"
 * logic from TurnService — TurnService only fires the event; this listener decides
 * what the arrival means for the collectibles module.
 */
@Component
public class TravelEventListener {

    private final CollectibleRepository collectibleRepository;
    private final ActiveEventRepository activeEventRepository;
    private final CharacterRepository characterRepository;
    private final Random random = new Random();

    public TravelEventListener(CollectibleRepository collectibleRepository,
                               ActiveEventRepository activeEventRepository,
                               CharacterRepository characterRepository) {
        this.collectibleRepository = collectibleRepository;
        this.activeEventRepository = activeEventRepository;
        this.characterRepository = characterRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTravelArrived(TravelArrivedEvent event) {
        List<Collectible> inCountry = collectibleRepository.findByCountryRequired(event.countryName());
        if (inCountry.isEmpty()) return;

        GameCharacter character = characterRepository.findByPlayerId(event.playerId()).orElse(null);
        if (character == null) return;
        int currentTurn = character.getCurrentTurn();

        List<ActiveEvent> existing = activeEventRepository.findActiveForPlayer(event.playerId(), currentTurn);

        // Pick a collectible not already on active sale
        List<Collectible> candidates = inCountry.stream()
            .filter(c -> existing.stream().noneMatch(e -> c.getId().equals(e.getCollectibleId())))
            .toList();

        if (candidates.isEmpty()) return;

        Collectible chosen = candidates.get(random.nextInt(candidates.size()));
        ActiveEvent saleEvent = new ActiveEvent();
        saleEvent.setPlayerId(event.playerId());
        saleEvent.setType("COLLECTIBLE_SALE");
        saleEvent.setCountry(event.countryName());
        saleEvent.setCollectibleId(chosen.getId());
        saleEvent.setExpiresAtTurn(currentTurn + 3);
        activeEventRepository.save(saleEvent);
    }
}
