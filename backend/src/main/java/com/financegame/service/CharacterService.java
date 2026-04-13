package com.financegame.service;

import com.financegame.dto.CharacterDto;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.MonthlyExpense;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.MonthlyExpenseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CharacterService {

    // Needs decay per month (applied every turn)
    private static final int HUNGER_DECAY_BASE    = 15;
    private static final int HUNGER_DECAY_WITH_FOOD = 5;  // food expense active
    private static final int ENERGY_DECAY         = 8;
    private static final int HAPPINESS_DECAY      = 5;
    // Stress is set by active jobs (see TurnService in Step 4)

    private final CharacterRepository characterRepository;
    private final MonthlyExpenseRepository monthlyExpenseRepository;

    public CharacterService(CharacterRepository characterRepository,
                            MonthlyExpenseRepository monthlyExpenseRepository) {
        this.characterRepository = characterRepository;
        this.monthlyExpenseRepository = monthlyExpenseRepository;
    }

    @Transactional(readOnly = true)
    public CharacterDto getCharacter(Long playerId) {
        GameCharacter character = findOrThrow(playerId);
        return CharacterDto.from(character);
    }

    /**
     * Apply one month of passive needs decay.
     * Called by the turn engine (Step 4). Food expense reduces hunger decay.
     */
    @Transactional
    public GameCharacter applyNeedsDecay(Long playerId) {
        GameCharacter character = findOrThrow(playerId);

        boolean hasFoodExpense = monthlyExpenseRepository.findActiveByPlayerId(playerId)
            .stream()
            .anyMatch(e -> "ESSEN".equals(e.getCategory()));

        int hungerDecay = hasFoodExpense ? HUNGER_DECAY_WITH_FOOD : HUNGER_DECAY_BASE;

        character.setHunger(clamp(character.getHunger() - hungerDecay));
        character.setEnergy(clamp(character.getEnergy() - ENERGY_DECAY));
        character.setHappiness(clamp(character.getHappiness() - HAPPINESS_DECAY));

        return characterRepository.save(character);
    }

    /**
     * Recalculate and persist net worth = cash + sum of investment current values.
     * Full investment lookup added in Step 8; for now net worth = cash.
     */
    @Transactional
    public void recalculateNetWorth(Long playerId) {
        GameCharacter character = findOrThrow(playerId);
        // TODO Step 8: add investment values
        character.setNetWorth(character.getCash());
        characterRepository.save(character);
    }

    /**
     * Deduct cash and persist. Throws 400 if insufficient funds.
     */
    @Transactional
    public void deductCash(Long playerId, BigDecimal amount, String context) {
        GameCharacter character = findOrThrow(playerId);
        if (character.getCash().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Nicht genug Geld fuer: " + context);
        }
        character.setCash(character.getCash().subtract(amount));
        characterRepository.save(character);
    }

    /**
     * Add cash and persist.
     */
    @Transactional
    public void addCash(Long playerId, BigDecimal amount) {
        GameCharacter character = findOrThrow(playerId);
        character.setCash(character.getCash().add(amount));
        characterRepository.save(character);
    }

    public GameCharacter findOrThrow(Long playerId) {
        return characterRepository.findByPlayerId(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Charakter nicht gefunden"));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
