package com.financegame.domain.event;

import com.financegame.entity.GameCharacter;
import com.financegame.repository.PlayerJobRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GehaltsbonusEvent implements RandomGameEvent {

    private final PlayerJobRepository playerJobRepository;

    public GehaltsbonusEvent(PlayerJobRepository playerJobRepository) {
        this.playerJobRepository = playerJobRepository;
    }

    @Override
    public void tryApply(Long playerId, GameCharacter character, List<String> events) {
        if (playerJobRepository.findActiveByPlayerId(playerId).isEmpty()) return;
        if (ThreadLocalRandom.current().nextDouble() > 0.07) return;

        int amount = 200 + ThreadLocalRandom.current().nextInt(601);
        character.setCash(character.getCash().add(BigDecimal.valueOf(amount)));
        character.setHappiness(clamp(character.getHappiness() + 5));
        events.add("Gehaltsbonus! Dein Arbeitgeber zahlt dir " + amount + " € extra.");
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
