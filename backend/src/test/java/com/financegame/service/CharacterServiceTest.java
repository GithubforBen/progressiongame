package com.financegame.service;

import com.financegame.entity.GameCharacter;
import com.financegame.entity.Investment;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.InvestmentRepository;
import com.financegame.repository.MonthlyExpenseRepository;
import com.financegame.repository.PlayerRealEstateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock CharacterRepository characterRepository;
    @Mock MonthlyExpenseRepository monthlyExpenseRepository;
    @Mock InvestmentRepository investmentRepository;
    @Mock PlayerRealEstateRepository playerRealEstateRepository;

    CharacterService service;

    @BeforeEach
    void setUp() {
        service = new CharacterService(characterRepository, monthlyExpenseRepository, investmentRepository, playerRealEstateRepository);
    }

    // ── deductCash ───────────────────────────────────────────────────────────

    @Test
    void deductCash_insufficientFunds_throws400() {
        when(characterRepository.findByPlayerId(1L)).thenReturn(Optional.of(characterWith(BigDecimal.valueOf(50))));

        assertThatThrownBy(() -> service.deductCash(1L, BigDecimal.valueOf(100), "Test"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Nicht genug Geld");
    }

    @Test
    void deductCash_exactAmount_succeeds() {
        GameCharacter character = characterWith(BigDecimal.valueOf(100));
        when(characterRepository.findByPlayerId(1L)).thenReturn(Optional.of(character));
        when(characterRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.deductCash(1L, BigDecimal.valueOf(100), "Test");

        assertThat(character.getCash()).isEqualByComparingTo("0.00");
        verify(characterRepository).save(character);
    }

    @Test
    void deductCash_partialAmount_subtractsCorrectly() {
        GameCharacter character = characterWith(BigDecimal.valueOf(500));
        when(characterRepository.findByPlayerId(1L)).thenReturn(Optional.of(character));
        when(characterRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.deductCash(1L, BigDecimal.valueOf(200), "Test");

        assertThat(character.getCash()).isEqualByComparingTo("300.00");
    }

    // ── addCash ──────────────────────────────────────────────────────────────

    @Test
    void addCash_increasesCash() {
        GameCharacter character = characterWith(BigDecimal.valueOf(100));
        when(characterRepository.findByPlayerId(1L)).thenReturn(Optional.of(character));
        when(characterRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.addCash(1L, BigDecimal.valueOf(250));

        assertThat(character.getCash()).isEqualByComparingTo("350.00");
    }

    // ── recalculateNetWorth ──────────────────────────────────────────────────

    @Test
    void recalculateNetWorth_sumsCashAndInvestments() {
        GameCharacter character = characterWith(BigDecimal.valueOf(1000));
        when(characterRepository.findByPlayerId(1L)).thenReturn(Optional.of(character));
        when(characterRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Investment inv1 = new Investment(); inv1.setCurrentValue(BigDecimal.valueOf(500));
        Investment inv2 = new Investment(); inv2.setCurrentValue(BigDecimal.valueOf(300));
        when(investmentRepository.findByPlayerId(1L)).thenReturn(List.of(inv1, inv2));

        service.recalculateNetWorth(1L);

        assertThat(character.getNetWorth()).isEqualByComparingTo("1800.00");
    }

    @Test
    void recalculateNetWorth_noInvestments_equalsOnlyCash() {
        GameCharacter character = characterWith(BigDecimal.valueOf(750));
        when(characterRepository.findByPlayerId(1L)).thenReturn(Optional.of(character));
        when(characterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(investmentRepository.findByPlayerId(1L)).thenReturn(List.of());

        service.recalculateNetWorth(1L);

        assertThat(character.getNetWorth()).isEqualByComparingTo("750.00");
    }

    // ── findOrThrow ──────────────────────────────────────────────────────────

    @Test
    void findOrThrow_characterNotFound_throws404() {
        when(characterRepository.findByPlayerId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOrThrow(99L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("nicht gefunden");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private GameCharacter characterWith(BigDecimal cash) {
        GameCharacter c = new GameCharacter();
        c.setPlayerId(1L);
        c.setCash(cash);
        c.setNetWorth(cash);
        return c;
    }
}
