package com.financegame.domain.event;

import com.financegame.entity.GameCharacter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DiebstahlEvent implements RandomGameEvent {

    @Override
    public void tryApply(Long playerId, GameCharacter character, List<String> events) {
        if (ThreadLocalRandom.current().nextDouble() > 0.05) return;

        BigDecimal cashNow = character.getCash();
        if (cashNow.compareTo(BigDecimal.TEN) <= 0) return;

        double pct = 0.10 + ThreadLocalRandom.current().nextDouble() * 0.20;
        BigDecimal stolen = cashNow.multiply(BigDecimal.valueOf(pct))
            .min(BigDecimal.valueOf(400))
            .setScale(2, RoundingMode.HALF_UP);

        character.setCash(cashNow.subtract(stolen));
        character.setHappiness(clamp(character.getHappiness() - 15));
        character.setStress(clamp(character.getStress() + 5));
        events.add("Einbruch! Diebe stehlen " + stolen + " € von dir.");
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
