package com.financegame.controller;

import com.financegame.dto.MonthlyExpenseDto;
import com.financegame.entity.MonthlyExpense;
import com.financegame.repository.MonthlyExpenseRepository;
import com.financegame.security.PlayerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class MonthlyExpenseController {

    private final MonthlyExpenseRepository monthlyExpenseRepository;

    public MonthlyExpenseController(MonthlyExpenseRepository monthlyExpenseRepository) {
        this.monthlyExpenseRepository = monthlyExpenseRepository;
    }

    @GetMapping
    public List<MonthlyExpenseDto> getExpenses(@AuthenticationPrincipal PlayerPrincipal principal) {
        return monthlyExpenseRepository.findByPlayerId(principal.id())
            .stream()
            .map(MonthlyExpenseDto::from)
            .toList();
    }

    @PatchMapping("/{id}/toggle")
    public MonthlyExpenseDto toggleExpense(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        MonthlyExpense expense = monthlyExpenseRepository.findByPlayerId(principal.id())
            .stream()
            .filter(e -> e.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ausgabe nicht gefunden"));

        if (expense.isMandatory()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pflichtausgaben koennen nicht deaktiviert werden");
        }

        expense.setActive(!expense.isActive());
        monthlyExpenseRepository.save(expense);
        return MonthlyExpenseDto.from(expense);
    }
}
