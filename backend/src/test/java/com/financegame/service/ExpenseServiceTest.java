package com.financegame.service;

import com.financegame.entity.MonthlyExpense;
import com.financegame.repository.MonthlyExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock MonthlyExpenseRepository monthlyExpenseRepository;

    ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(monthlyExpenseRepository);
    }

    // ── toggleExpense ────────────────────────────────────────────────────────

    @Test
    void toggleExpense_mandatory_throws400() {
        MonthlyExpense expense = expense(1L, true, true);
        when(monthlyExpenseRepository.findByPlayerId(10L)).thenReturn(List.of(expense));

        assertThatThrownBy(() -> service.toggleExpense(10L, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Pflichtausgaben");
    }

    @Test
    void toggleExpense_activeOptional_deactivates() {
        MonthlyExpense expense = expense(1L, true, false);
        when(monthlyExpenseRepository.findByPlayerId(10L)).thenReturn(List.of(expense));
        when(monthlyExpenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.toggleExpense(10L, 1L);

        assertThat(result.active()).isFalse();
    }

    @Test
    void toggleExpense_inactiveOptional_activates() {
        MonthlyExpense expense = expense(1L, false, false);
        when(monthlyExpenseRepository.findByPlayerId(10L)).thenReturn(List.of(expense));
        when(monthlyExpenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.toggleExpense(10L, 1L);

        assertThat(result.active()).isTrue();
    }

    @Test
    void toggleExpense_notFound_throws404() {
        when(monthlyExpenseRepository.findByPlayerId(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.toggleExpense(10L, 99L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("nicht gefunden");
    }

    // ── deleteExpense ────────────────────────────────────────────────────────

    @Test
    void deleteExpense_mandatory_throws400() {
        MonthlyExpense expense = expense(1L, true, true);
        when(monthlyExpenseRepository.findByPlayerId(10L)).thenReturn(List.of(expense));

        assertThatThrownBy(() -> service.deleteExpense(10L, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Pflichtausgaben");
    }

    @Test
    void deleteExpense_optional_callsDelete() {
        MonthlyExpense expense = expense(2L, true, false);
        when(monthlyExpenseRepository.findByPlayerId(10L)).thenReturn(List.of(expense));

        service.deleteExpense(10L, 2L);

        verify(monthlyExpenseRepository).delete(2L);
    }

    // ── addExpense ───────────────────────────────────────────────────────────

    @Test
    void addExpense_invalidCategory_throws400() {
        assertThatThrownBy(() -> service.addExpense(10L, "UNKNOWN", "Label", BigDecimal.TEN))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Ungueltiger Kategorie-Typ");
    }

    @Test
    void addExpense_zeroAmount_throws400() {
        assertThatThrownBy(() -> service.addExpense(10L, "GYM", "Fitnessstudio", BigDecimal.ZERO))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("groesser als 0");
    }

    @Test
    void addExpense_blankLabel_throws400() {
        assertThatThrownBy(() -> service.addExpense(10L, "GYM", "  ", BigDecimal.TEN))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("leer");
    }

    @Test
    void addExpense_valid_savesAndReturns() {
        when(monthlyExpenseRepository.save(any())).thenAnswer(i -> {
            MonthlyExpense e = i.getArgument(0);
            e.setId(5L);
            return e;
        });

        var result = service.addExpense(10L, "GYM", "Fitnessstudio", new BigDecimal("29.99"));

        assertThat(result.category()).isEqualTo("GYM");
        assertThat(result.label()).isEqualTo("Fitnessstudio");
        assertThat(result.amount()).isEqualByComparingTo("29.99");
        assertThat(result.active()).isTrue();
        verify(monthlyExpenseRepository).save(any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private MonthlyExpense expense(Long id, boolean active, boolean mandatory) {
        MonthlyExpense e = new MonthlyExpense();
        e.setId(id);
        e.setPlayerId(10L);
        e.setCategory("ESSEN");
        e.setLabel("Test");
        e.setAmount(BigDecimal.TEN);
        e.setActive(active);
        e.setMandatory(mandatory);
        return e;
    }
}
