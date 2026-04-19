package com.financegame.domain.event;

import com.financegame.entity.GameCharacter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class StressabbauEvent implements RandomGameEvent {

    @Override
    public void tryApply(Long playerId, GameCharacter character, List<String> events) {
        if (ThreadLocalRandom.current().nextDouble() > 0.06) return;

        character.setStress(clamp(character.getStress() - 25));
        character.setEnergy(clamp(character.getEnergy() + 15));
        character.setHappiness(clamp(character.getHappiness() + 10));
        events.add("Entspannter Monat! Stress sinkt, Energie und Glück steigen.");
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
