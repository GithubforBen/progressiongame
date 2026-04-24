package com.financegame.controller;

import com.financegame.dto.BuyStockRequest;
import com.financegame.dto.InvestmentDto;
import com.financegame.dto.SellStockResponse;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.InvestmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @GetMapping
    public List<InvestmentDto> getPortfolio(@AuthenticationPrincipal PlayerPrincipal principal) {
        return investmentService.getPortfolio(principal.id());
    }

    @PostMapping("/stocks/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentDto buyStock(
        @RequestBody BuyStockRequest request,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return investmentService.buyStock(principal.id(), request.ticker(), request.quantity());
    }

    @PostMapping("/{id}/sell")
    public SellStockResponse sellStock(
        @PathVariable Long id,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return investmentService.sellStock(principal.id(), id);
    }
}
