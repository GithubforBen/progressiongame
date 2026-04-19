package com.financegame.service;

import com.financegame.config.GameConfig;
import com.financegame.dto.CharacterDto;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.Investment;
import com.financegame.entity.NeedsItem;
import com.financegame.entity.PlayerRealEstate;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.InvestmentRepository;
import com.financegame.repository.MonthlyExpenseRepository;
import com.financegame.repository.NeedsItemRepository;
import com.financegame.repository.PlayerRealEstateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final MonthlyExpenseRepository monthlyExpenseRepository;
    private final InvestmentRepository investmentRepository;
    private final PlayerRealEstateRepository playerRealEstateRepository;
    private final NeedsItemRepository needsItemRepository;
    private final GameConfig gameConfig;

    public CharacterService(CharacterRepository characterRepository,
                            MonthlyExpenseRepository monthlyExpenseRepository,
                            InvestmentRepository investmentRepository,
                            PlayerRealEstateRepository playerRealEstateRepository,
                            NeedsItemRepository needsItemRepository,
                            GameConfig gameConfig) {
        this.characterRepository = characterRepository;
        this.monthlyExpenseRepository = monthlyExpenseRepository;
        this.investmentRepository = investmentRepository;
        this.playerRealEstateRepository = playerRealEstateRepository;
        this.needsItemRepository = needsItemRepository;
        this.gameConfig = gameConfig;
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

        GameConfig.NeedsConfig needs = gameConfig.getNeeds();
        int hungerDecay = hasFoodExpense ? needs.getHungerDecayWithFood() : needs.getHungerDecayBase();

        character.setHunger(clamp(character.getHunger() - hungerDecay));
        character.setEnergy(clamp(character.getEnergy() - needs.getEnergyDecay()));
        character.setHappiness(clamp(character.getHappiness() - needs.getHappinessDecay()));

        return characterRepository.save(character);
    }

    /**
     * Recalculate and persist net worth = cash + sum of investment current values.
     */
    @Transactional
    public void recalculateNetWorth(Long playerId) {
        GameCharacter character = findOrThrow(playerId);
        BigDecimal investmentValue = investmentRepository.findByPlayerId(playerId)
            .stream()
            .map(Investment::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realEstateValue = playerRealEstateRepository.findByPlayerId(playerId)
            .stream()
            .map(PlayerRealEstate::getPurchasePrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        character.setNetWorth(character.getCash().add(investmentValue).add(realEstateValue));
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

    @Transactional
    public void updateSchufaScore(Long playerId, int newScore) {
        GameCharacter character = findOrThrow(playerId);
        character.setSchufaScore(Math.max(0, Math.min(1000, newScore)));
        characterRepository.save(character);
    }

    @Transactional
    public CharacterDto purchaseNeedItem(Long playerId, String itemId) {
        NeedsItem item = needsItemRepository.findById(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item nicht gefunden: " + itemId));

        GameCharacter character = findOrThrow(playerId);

        if (item.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            if (character.getCash().compareTo(item.getPrice()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nicht genug Geld");
            }
            character.setCash(character.getCash().subtract(item.getPrice()));
        }

        character.setHunger(clamp(character.getHunger() + item.getHungerEffect()));
        character.setEnergy(clamp(character.getEnergy() + item.getEnergyEffect()));
        character.setHappiness(clamp(character.getHappiness() + item.getHappinessEffect()));
        character.setStress(clamp(character.getStress() + item.getStressEffect()));

        if (item.isDepressionReduction() && character.getDepressionMonthsRemaining() > 0) {
            character.setDepressionMonthsRemaining(character.getDepressionMonthsRemaining() - 1);
        }

        characterRepository.save(character);
        return CharacterDto.from(character);
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
