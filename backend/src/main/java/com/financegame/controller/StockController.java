package com.financegame.controller;

import com.financegame.dto.StockDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.StockService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public List<StockDto> getAllStocks(@AuthenticationPrincipal PlayerPrincipal principal) {
        return stockService.getAllStocks(principal.id());
    }
}
