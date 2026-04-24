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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class CharacterService {

    @PersistenceContext
    private EntityManager em;

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

    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("-5000.00");

    /**
     * Deduct cash and persist. Allows overdraft down to OVERDRAFT_LIMIT (-5000).
     */
    @Transactional
    public void deductCash(Long playerId, BigDecimal amount, String context) {
        GameCharacter character = findOrThrow(playerId);
        BigDecimal newBalance = character.getCash().subtract(amount);
        if (newBalance.compareTo(OVERDRAFT_LIMIT) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Nicht genug Geld. Benötigt: " + amount.setScale(2, java.math.RoundingMode.HALF_UP)
                + " €, Verfügbar: " + character.getCash().setScale(2, java.math.RoundingMode.HALF_UP) + " €"
                + " (Kreditlimit: " + OVERDRAFT_LIMIT.abs() + " €)");
        }
        character.setCash(newBalance);
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

    @Transactional
    public void resetCharacter(Long playerId) {
        GameCharacter character = findOrThrow(playerId);
        BigDecimal personalBest = character.getPersonalBestNetWorth();

        // Purge all player-specific game data
        em.createNativeQuery("DELETE FROM investments WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_real_estate WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM monthly_expenses WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_lifestyle_items WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_loans WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM monthly_snapshots WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_collectibles WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_relationships WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_social_relationships WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_social_group_unlocks WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_travel WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM player_investment_levels WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM job_applications WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM active_events WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM events_log WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM gambling_sessions WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM social_action_log WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();
        em.createNativeQuery("DELETE FROM education_progress WHERE player_id = :pid").setParameter("pid", playerId).executeUpdate();

        // Reset character to starting values, preserving personal best
        character.setCash(new BigDecimal("1000.00"));
        character.setNetWorth(new BigDecimal("1000.00"));
        character.setStress(0);
        character.setHunger(100);
        character.setEnergy(100);
        character.setHappiness(70);
        character.setCurrentTurn(1);
        character.setSchufaScore(500);
        character.setDepressionMonthsRemaining(0);
        character.setBurnoutActive(false);
        character.setTaxEvasionActive(false);
        character.setTaxEvasionCaughtPending(false);
        character.setCumulativeEvadedTaxes(BigDecimal.ZERO);
        character.setJailMonthsRemaining(0);
        character.setExileMonthsRemaining(0);
        character.setTotalJailMonthsServed(0);
        character.setVictoryAchieved(false);
        character.setPersonalBestNetWorth(personalBest);
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
