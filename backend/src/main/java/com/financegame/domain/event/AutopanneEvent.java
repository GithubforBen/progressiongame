package com.financegame.domain.event;

import com.financegame.entity.GameCharacter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AutopanneEvent implements RandomGameEvent {

    @Override
    public void tryApply(Long playerId, GameCharacter character, List<String> events) {
        if (ThreadLocalRandom.current().nextDouble() > 0.08) return;

        int cost = 150 + ThreadLocalRandom.current().nextInt(351);
        character.setCash(character.getCash().subtract(BigDecimal.valueOf(cost)).max(BigDecimal.ZERO));
        character.setStress(clamp(character.getStress() + 10));
        events.add("Autopanne! Die Reparatur kostet dich " + cost + " €.");
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
