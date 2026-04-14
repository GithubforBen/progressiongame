package com.financegame.controller;

import com.financegame.dto.LoanDto;
import com.financegame.dto.SchufaDto;
import com.financegame.dto.TakeLoanRequest;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public List<LoanDto> getLoans(@AuthenticationPrincipal PlayerPrincipal principal) {
        return loanService.getLoans(principal.id());
    }

    @GetMapping("/schufa")
    public SchufaDto getSchufa(@AuthenticationPrincipal PlayerPrincipal principal) {
        return loanService.getSchufa(principal.id());
    }

    @PostMapping("/take")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanDto takeLoan(
        @RequestBody TakeLoanRequest req,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return loanService.takeLoan(principal.id(), req);
    }
}
