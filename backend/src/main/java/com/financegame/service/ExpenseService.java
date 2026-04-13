package com.financegame.service;

import com.financegame.dto.MonthlyExpenseDto;
import com.financegame.entity.MonthlyExpense;
import com.financegame.repository.MonthlyExpenseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class ExpenseService {

    // Allowed categories a player can manually add
    public static final Set<String> ALLOWED_CATEGORIES = Set.of(
        "GYM", "STREAMING", "KRANKENVERSICHERUNG", "MOBILFUNK",
        "INTERNET", "ZEITSCHRIFTEN", "SONSTIGES"
    );

    private final MonthlyExpenseRepository monthlyExpenseRepository;

    public ExpenseService(MonthlyExpenseRepository monthlyExpenseRepository) {
        this.monthlyExpenseRepository = monthlyExpenseRepository;
    }

    @Transactional(readOnly = true)
    public List<MonthlyExpenseDto> getExpenses(Long playerId) {
        return monthlyExpenseRepository.findByPlayerId(playerId)
            .stream()
            .map(MonthlyExpenseDto::from)
            .toList();
    }

    @Transactional
    public MonthlyExpenseDto toggleExpense(Long playerId, Long expenseId) {
        MonthlyExpense expense = findOwned(playerId, expenseId);
        if (expense.isMandatory()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Pflichtausgaben koennen nicht deaktiviert werden");
        }
        expense.setActive(!expense.isActive());
        monthlyExpenseRepository.save(expense);
        return MonthlyExpenseDto.from(expense);
    }

    @Transactional
    public MonthlyExpenseDto addExpense(Long playerId, String category, String label, BigDecimal amount) {
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ungueltiger Kategorie-Typ: " + category);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Betrag muss groesser als 0 sein");
        }
        if (label == null || label.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bezeichnung darf nicht leer sein");
        }

        MonthlyExpense expense = new MonthlyExpense();
        expense.setPlayerId(playerId);
        expense.setCategory(category);
        expense.setLabel(label.trim());
        expense.setAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
        expense.setActive(true);
        expense.setMandatory(false);
        monthlyExpenseRepository.save(expense);
        return MonthlyExpenseDto.from(expense);
    }

    @Transactional
    public void deleteExpense(Long playerId, Long expenseId) {
        MonthlyExpense expense = findOwned(playerId, expenseId);
        if (expense.isMandatory()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Pflichtausgaben koennen nicht geloescht werden");
        }
        monthlyExpenseRepository.delete(expenseId);
    }

    private MonthlyExpense findOwned(Long playerId, Long expenseId) {
        return monthlyExpenseRepository.findByPlayerId(playerId)
            .stream()
            .filter(e -> e.getId().equals(expenseId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Ausgabe nicht gefunden"));
    }
}
