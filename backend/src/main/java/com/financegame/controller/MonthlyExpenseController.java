package com.financegame.controller;

import com.financegame.dto.AddExpenseRequest;
import com.financegame.dto.MonthlyExpenseDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class MonthlyExpenseController {

    private final ExpenseService expenseService;

    public MonthlyExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<MonthlyExpenseDto> getExpenses(@AuthenticationPrincipal PlayerPrincipal principal) {
        return expenseService.getExpenses(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonthlyExpenseDto addExpense(
        @RequestBody AddExpenseRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return expenseService.addExpense(principal.id(), request.category(), request.label(), request.amount());
    }

    @PatchMapping("/{id}/toggle")
    public MonthlyExpenseDto toggleExpense(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return expenseService.toggleExpense(principal.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        expenseService.deleteExpense(principal.id(), id);
    }
}
