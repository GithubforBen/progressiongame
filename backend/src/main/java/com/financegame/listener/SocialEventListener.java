package com.financegame.listener;

import com.financegame.domain.events.GroupUnlockedEvent;
import com.financegame.domain.events.RelationshipChangedEvent;
import com.financegame.domain.events.RobAttemptedEvent;
import com.financegame.entity.EventLog;
import com.financegame.repository.EventLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SocialEventListener {

    private final EventLogRepository eventLogRepository;

    public SocialEventListener(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRelationshipChanged(RelationshipChangedEvent event) {
        String msg = String.format("Beziehung zu %s geändert: %d → %d (%s)",
            event.personId(), event.oldScore(), event.newScore(), event.reason());
        eventLogRepository.save(new EventLog(event.playerId(), msg, null, "SOCIAL", 0));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onGroupUnlocked(GroupUnlockedEvent event) {
        String msg = "Neue Kontakte verfügbar: Gruppe \"" + event.groupName() + "\" freigeschaltet!";
        eventLogRepository.save(new EventLog(event.playerId(), msg, null, "SOCIAL", 0));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRobAttempted(RobAttemptedEvent event) {
        String outcome = event.caught() ? "ERWISCHT" : (event.success() ? "ERFOLG" : "GESCHEITERT");
        String msg = String.format("Ausraubversuch bei %s: %s%s",
            event.personId(), outcome,
            event.success() ? " (+" + event.lootAmount().toPlainString() + "€)" : "");
        eventLogRepository.save(new EventLog(event.playerId(), msg, null, "SOCIAL", 0));
    }
}
