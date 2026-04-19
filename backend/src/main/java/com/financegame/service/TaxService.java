package com.financegame.service;

import com.financegame.config.GameConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TaxService {

    private final GameConfig gameConfig;

    public TaxService(GameConfig gameConfig) {
        this.gameConfig = gameConfig;
    }

    public BigDecimal calculateTax(BigDecimal income) {
        List<GameConfig.TaxConfig.TaxBracket> brackets = gameConfig.getTax().getBrackets();
        BigDecimal tax = BigDecimal.ZERO;
        double prev = 0;
        for (GameConfig.TaxConfig.TaxBracket bracket : brackets) {
            double incomeD = income.doubleValue();
            if (incomeD <= prev) break;
            double cap = Math.min(incomeD, bracket.getUpTo());
            double marginal = cap - prev;
            if (marginal > 0 && bracket.getRate() > 0) {
                tax = tax.add(BigDecimal.valueOf(marginal * bracket.getRate()));
            }
            prev = bracket.getUpTo();
        }
        return tax.setScale(2, RoundingMode.HALF_UP);
    }

    public String determineBracketLabel(BigDecimal income) {
        for (GameConfig.TaxConfig.TaxBracket bracket : gameConfig.getTax().getBrackets()) {
            if (income.doubleValue() <= bracket.getUpTo()) {
                return bracket.getLabel();
            }
        }
        List<GameConfig.TaxConfig.TaxBracket> b = gameConfig.getTax().getBrackets();
        return b.isEmpty() ? "" : b.get(b.size() - 1).getLabel();
    }

    public int determineBracketPercent(BigDecimal income) {
        for (GameConfig.TaxConfig.TaxBracket bracket : gameConfig.getTax().getBrackets()) {
            if (income.doubleValue() <= bracket.getUpTo()) {
                return (int) Math.round(bracket.getRate() * 100);
            }
        }
        List<GameConfig.TaxConfig.TaxBracket> b = gameConfig.getTax().getBrackets();
        return b.isEmpty() ? 0 : (int) Math.round(b.get(b.size() - 1).getRate() * 100);
    }
}
