package com.financegame.domain.event;

import com.financegame.entity.GameCharacter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GluecksfallEvent implements RandomGameEvent {

    @Override
    public void tryApply(Long playerId, GameCharacter character, List<String> events) {
        if (ThreadLocalRandom.current().nextDouble() > 0.05) return;

        int amount = 30 + ThreadLocalRandom.current().nextInt(221);
        character.setCash(character.getCash().add(BigDecimal.valueOf(amount)));
        character.setHappiness(clamp(character.getHappiness() + 10));
        events.add("Glücksfall! Du findest unerwartet " + amount + " €.");
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
