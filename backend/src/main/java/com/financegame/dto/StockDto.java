package com.financegame.dto;

import com.financegame.entity.Stock;
import com.financegame.entity.StockPriceHistory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record StockDto(
    Long id,
    String name,
    String ticker,
    String type,
    BigDecimal currentPrice,
    BigDecimal priceChangePct,   // vs. last turn, null if no history
    List<PricePointDto> history,
    String requiredCert,
    boolean locked,
    boolean delisted
) {
    public record PricePointDto(BigDecimal price, int turn) {}

    public static StockDto from(Stock stock, List<StockPriceHistory> history, List<String> completedStages, boolean delisted) {
        List<PricePointDto> points = history.stream()
            .map(h -> new PricePointDto(h.getPrice(), h.getTurn()))
            .toList();

        BigDecimal currentPrice = history.isEmpty()
            ? stock.getCurrentPrice()
            : history.get(history.size() - 1).getPrice();

        BigDecimal changePct = null;
        if (history.size() >= 2) {
            BigDecimal prev = history.get(history.size() - 2).getPrice();
            if (prev.compareTo(BigDecimal.ZERO) != 0) {
                changePct = currentPrice
                    .subtract(prev)
                    .divide(prev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            }
        }

        String req = stock.getRequiredCert();
        boolean locked = req != null && !completedStages.contains(req);

        return new StockDto(
            stock.getId(),
            stock.getName(),
            stock.getTicker(),
            stock.getType(),
            currentPrice,
            changePct,
            points,
            req,
            locked,
            delisted
        );
    }
}
