package com.financegame.controller;

import com.financegame.dto.TaxPreviewDto;
import com.financegame.entity.PlayerJob;
import com.financegame.repository.PlayerJobRepository;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.TaxService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/tax")
public class TaxController {

    private final PlayerJobRepository playerJobRepository;
    private final TaxService taxService;

    public TaxController(PlayerJobRepository playerJobRepository, TaxService taxService) {
        this.playerJobRepository = playerJobRepository;
        this.taxService = taxService;
    }

    @GetMapping("/preview")
    public TaxPreviewDto preview(@AuthenticationPrincipal PlayerPrincipal principal) {
        List<PlayerJob> active = playerJobRepository.findActiveByPlayerId(principal.id());
        BigDecimal gross = active.stream()
            .map(pj -> pj.getJob().getSalary())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = taxService.calculateTax(gross);
        BigDecimal net = gross.subtract(tax);
        String bracket = taxService.determineBracketLabel(gross);
        int percent = taxService.determineBracketPercent(gross);

        return new TaxPreviewDto(gross, tax, net, bracket, percent);
    }
}
