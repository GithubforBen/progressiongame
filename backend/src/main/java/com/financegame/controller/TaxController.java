package com.financegame.controller;

import com.financegame.dto.TaxPreviewDto;
import com.financegame.entity.PlayerJob;
import com.financegame.repository.PlayerJobRepository;
import com.financegame.security.PlayerPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequestMapping("/api/tax")
public class TaxController {

    private final PlayerJobRepository playerJobRepository;

    public TaxController(PlayerJobRepository playerJobRepository) {
        this.playerJobRepository = playerJobRepository;
    }

    @GetMapping("/preview")
    public TaxPreviewDto preview(@AuthenticationPrincipal PlayerPrincipal principal) {
        List<PlayerJob> active = playerJobRepository.findActiveByPlayerId(principal.id());
        BigDecimal gross = active.stream()
            .map(pj -> pj.getJob().getSalary())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = calculateTax(gross);
        BigDecimal net = gross.subtract(tax);

        String bracket = determineBracket(gross);
        int percent = determineBracketPercent(gross);

        return new TaxPreviewDto(gross, tax, net, bracket, percent);
    }

    private BigDecimal calculateTax(BigDecimal income) {
        if (income.compareTo(BigDecimal.valueOf(1000)) <= 0) return BigDecimal.ZERO;

        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal[] brackets = {
            BigDecimal.valueOf(1000), BigDecimal.valueOf(2000), BigDecimal.valueOf(3000)
        };
        double[] rates = {0.20, 0.32, 0.42};

        BigDecimal remaining = income.subtract(BigDecimal.valueOf(1000));
        BigDecimal prev = BigDecimal.ZERO;
        for (int i = 0; i < brackets.length; i++) {
            BigDecimal top = brackets[i];
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal taxable = remaining.min(top.subtract(prev));
            tax = tax.add(taxable.multiply(BigDecimal.valueOf(rates[i])));
            remaining = remaining.subtract(taxable);
            prev = top;
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            tax = tax.add(remaining.multiply(BigDecimal.valueOf(0.42)));
        }
        return tax.setScale(2, RoundingMode.HALF_UP);
    }

    private String determineBracket(BigDecimal income) {
        if (income.compareTo(BigDecimal.valueOf(1000)) <= 0) return "0 – 1.000 €";
        if (income.compareTo(BigDecimal.valueOf(3000)) <= 0) return "1.001 – 3.000 €";
        if (income.compareTo(BigDecimal.valueOf(6000)) <= 0) return "3.001 – 6.000 €";
        return "Über 6.000 €";
    }

    private int determineBracketPercent(BigDecimal income) {
        if (income.compareTo(BigDecimal.valueOf(1000)) <= 0) return 0;
        if (income.compareTo(BigDecimal.valueOf(3000)) <= 0) return 20;
        if (income.compareTo(BigDecimal.valueOf(6000)) <= 0) return 32;
        return 42;
    }
}
