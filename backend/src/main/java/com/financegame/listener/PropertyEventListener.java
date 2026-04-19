package com.financegame.listener;

import com.financegame.domain.events.PropertyModeChangedEvent;
import com.financegame.domain.events.PropertyPurchasedEvent;
import com.financegame.entity.EventLog;
import com.financegame.entity.GameCharacter;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.EventLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Logs real-estate purchases and mode changes to EventLog.
 * These happen outside the turn cycle so TurnEventListener would miss them.
 */
@Component
public class PropertyEventListener {

    private final EventLogRepository eventLogRepository;
    private final CharacterRepository characterRepository;

    public PropertyEventListener(EventLogRepository eventLogRepository,
                                 CharacterRepository characterRepository) {
        this.eventLogRepository = eventLogRepository;
        this.characterRepository = characterRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPropertyPurchased(PropertyPurchasedEvent event) {
        int turn = currentTurn(event.playerId());
        eventLogRepository.save(new EventLog(
            event.playerId(),
            "🏠 Immobilie gekauft: \"" + event.propertyName() + "\" für " + event.price() + " €",
            event.price().negate(), "REAL_ESTATE", turn
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPropertyModeChanged(PropertyModeChangedEvent event) {
        int turn = currentTurn(event.playerId());
        String label = "RENTED_OUT".equals(event.newMode()) ? "vermietet" : "selbst bewohnt";
        eventLogRepository.save(new EventLog(
            event.playerId(),
            "🔄 Immobilie #" + event.propertyId() + " jetzt: " + label,
            null, "REAL_ESTATE", turn
        ));
    }

    private int currentTurn(Long playerId) {
        return characterRepository.findByPlayerId(playerId)
            .map(GameCharacter::getCurrentTurn).orElse(0);
    }
}
