package com.financegame.listener;

import com.financegame.domain.events.StockPurchasedEvent;
import com.financegame.domain.events.StockSoldEvent;
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
 * Logs stock buy and sell events to EventLog.
 * These happen outside the turn cycle so TurnEventListener would miss them.
 */
@Component
public class InvestmentEventListener {

    private final EventLogRepository eventLogRepository;
    private final CharacterRepository characterRepository;

    public InvestmentEventListener(EventLogRepository eventLogRepository,
                                   CharacterRepository characterRepository) {
        this.eventLogRepository = eventLogRepository;
        this.characterRepository = characterRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockPurchased(StockPurchasedEvent event) {
        int turn = currentTurn(event.playerId());
        eventLogRepository.save(new EventLog(
            event.playerId(),
            "📈 Aktie gekauft: " + event.ticker() + " × " + event.quantity() + " für " + event.totalCost() + " €",
            event.totalCost().negate(), "INVESTMENT", turn
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockSold(StockSoldEvent event) {
        int turn = currentTurn(event.playerId());
        String sign = event.profitLoss().signum() >= 0 ? "+" : "";
        eventLogRepository.save(new EventLog(
            event.playerId(),
            "📉 Aktie verkauft: " + event.ticker() + " — Erlös: " + event.proceeds()
                + " € (" + sign + event.profitLoss() + " €)",
            event.proceeds(), "INVESTMENT", turn
        ));
    }

    private int currentTurn(Long playerId) {
        return characterRepository.findByPlayerId(playerId)
            .map(GameCharacter::getCurrentTurn).orElse(0);
    }
}
