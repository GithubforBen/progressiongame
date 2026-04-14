package com.financegame.service;

import com.financegame.entity.GameCharacter;
import com.financegame.repository.MonthlyExpenseRepository;
import com.financegame.repository.PlayerJobRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * Step 11 – Random Events
 * Each event is checked independently with its own probability.
 * Events modify cash and/or character stats directly.
 */
@Service
public class RandomEventService {

    private final MonthlyExpenseRepository monthlyExpenseRepository;
    private final PlayerJobRepository playerJobRepository;
    private final Random random = new Random();

    public RandomEventService(MonthlyExpenseRepository monthlyExpenseRepository,
                              PlayerJobRepository playerJobRepository) {
        this.monthlyExpenseRepository = monthlyExpenseRepository;
        this.playerJobRepository = playerJobRepository;
    }

    /**
     * Applies zero or more random events to the character this turn.
     * All cash changes are applied directly to character.cash.
     * The caller (TurnService) is responsible for persisting the character.
     */
    public void applyRandomEvents(Long playerId, GameCharacter character, List<String> events) {
        applyGehaltsbonus(playerId, character, events);
        applyAutopanne(character, events);
        applyGluecksfall(character, events);
        applyDiebstahl(character, events);
        applyStressabbau(character, events);
        applyUnerwarteteRechnung(character, events);
    }

    // -------------------------------------------------------------------------
    // Event: Gehaltsbonus (7 % – nur wenn aktive Jobs vorhanden)
    // -------------------------------------------------------------------------
    private void applyGehaltsbonus(Long playerId, GameCharacter character, List<String> events) {
        boolean hasJob = !playerJobRepository.findActiveByPlayerId(playerId).isEmpty();
        if (!hasJob || random.nextDouble() > 0.07) return;

        int amount = 200 + random.nextInt(601); // 200–800 €
        character.setCash(character.getCash().add(BigDecimal.valueOf(amount)));
        character.setHappiness(clamp(character.getHappiness() + 5));
        events.add("Gehaltsbonus! Dein Arbeitgeber zahlt dir " + amount + " € extra.");
    }

    // -------------------------------------------------------------------------
    // Event: Autopanne (8 %)
    // -------------------------------------------------------------------------
    private void applyAutopanne(GameCharacter character, List<String> events) {
        if (random.nextDouble() > 0.08) return;

        int cost = 150 + random.nextInt(351); // 150–500 €
        character.setCash(character.getCash().subtract(BigDecimal.valueOf(cost)).max(BigDecimal.ZERO));
        character.setStress(clamp(character.getStress() + 10));
        events.add("Autopanne! Die Reparatur kostet dich " + cost + " €.");
    }

    // -------------------------------------------------------------------------
    // Event: Glücksfall (5 %)
    // -------------------------------------------------------------------------
    private void applyGluecksfall(GameCharacter character, List<String> events) {
        if (random.nextDouble() > 0.05) return;

        int amount = 30 + random.nextInt(221); // 30–250 €
        character.setCash(character.getCash().add(BigDecimal.valueOf(amount)));
        character.setHappiness(clamp(character.getHappiness() + 10));
        events.add("Glücksfall! Du findest unerwartet " + amount + " €.");
    }

    // -------------------------------------------------------------------------
    // Event: Diebstahl (5 %)
    // -------------------------------------------------------------------------
    private void applyDiebstahl(GameCharacter character, List<String> events) {
        if (random.nextDouble() > 0.05) return;

        // 10–30 % des aktuellen Bargelds, maximal 400 €
        BigDecimal cashNow = character.getCash();
        if (cashNow.compareTo(BigDecimal.TEN) <= 0) return; // Nichts zu stehlen

        double pct = 0.10 + random.nextDouble() * 0.20;
        BigDecimal stolen = cashNow.multiply(BigDecimal.valueOf(pct)).min(BigDecimal.valueOf(400));
        stolen = stolen.setScale(2, java.math.RoundingMode.HALF_UP);

        character.setCash(cashNow.subtract(stolen));
        character.setHappiness(clamp(character.getHappiness() - 15));
        character.setStress(clamp(character.getStress() + 5));
        events.add("Einbruch! Diebe stehlen " + stolen + " € von dir.");
    }

    // -------------------------------------------------------------------------
    // Event: Stressabbau (6 %)
    // -------------------------------------------------------------------------
    private void applyStressabbau(GameCharacter character, List<String> events) {
        if (random.nextDouble() > 0.06) return;

        character.setStress(clamp(character.getStress() - 25));
        character.setEnergy(clamp(character.getEnergy() + 15));
        character.setHappiness(clamp(character.getHappiness() + 10));
        events.add("Entspannter Monat! Stress sinkt, Energie und Glück steigen.");
    }

    // -------------------------------------------------------------------------
    // Event: Unerwartete Rechnung (7 %)
    // -------------------------------------------------------------------------
    private void applyUnerwarteteRechnung(GameCharacter character, List<String> events) {
        if (random.nextDouble() > 0.07) return;

        int cost = 100 + random.nextInt(201); // 100–300 €
        character.setCash(character.getCash().subtract(BigDecimal.valueOf(cost)).max(BigDecimal.ZERO));
        events.add("Unerwartete Rechnung! " + cost + " € Sonderkosten.");
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
