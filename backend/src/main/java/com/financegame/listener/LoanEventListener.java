package com.financegame.listener;

import com.financegame.domain.events.LoanDefaultedEvent;
import com.financegame.domain.events.LoanPaidOffEvent;
import com.financegame.domain.events.LoanTakenEvent;
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
 * Logs loan lifecycle events to EventLog. LoanTaken fires from user-action (outside turn),
 * PaidOff and Defaulted fire from within the turn. All three are logged here so that
 * loan history is visible in the event log without any service carrying EventLogRepository.
 */
@Component
public class LoanEventListener {

    private final EventLogRepository eventLogRepository;
    private final CharacterRepository characterRepository;

    public LoanEventListener(EventLogRepository eventLogRepository,
                             CharacterRepository characterRepository) {
        this.eventLogRepository = eventLogRepository;
        this.characterRepository = characterRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanTaken(LoanTakenEvent event) {
        int turn = currentTurn(event.playerId());
        eventLogRepository.save(new EventLog(
            event.playerId(),
            "💳 Kredit aufgenommen: \"" + event.label() + "\" über " + event.amount() + " €",
            event.amount(), "LOAN", turn
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanPaidOff(LoanPaidOffEvent event) {
        int turn = currentTurn(event.playerId());
        eventLogRepository.save(new EventLog(
            event.playerId(),
            "✅ Kredit vollständig abbezahlt: \"" + event.label() + "\"",
            null, "LOAN", turn
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanDefaulted(LoanDefaultedEvent event) {
        int turn = currentTurn(event.playerId());
        eventLogRepository.save(new EventLog(
            event.playerId(),
            "❌ Kreditausfall: \"" + event.label() + "\" — offener Betrag: " + event.amountRemaining() + " €",
            event.amountRemaining().negate(), "LOAN", turn
        ));
    }

    private int currentTurn(Long playerId) {
        return characterRepository.findByPlayerId(playerId)
            .map(GameCharacter::getCurrentTurn).orElse(0);
    }
}
