package com.financegame.domain.event;

import com.financegame.entity.GameCharacter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UnerwarteteRechnungEvent implements RandomGameEvent {

    @Override
    public void tryApply(Long playerId, GameCharacter character, List<String> events) {
        if (ThreadLocalRandom.current().nextDouble() > 0.07) return;

        int cost = 100 + ThreadLocalRandom.current().nextInt(201);
        character.setCash(character.getCash().subtract(BigDecimal.valueOf(cost)).max(BigDecimal.ZERO));
        events.add("Unerwartete Rechnung! " + cost + " € Sonderkosten.");
    }
}
