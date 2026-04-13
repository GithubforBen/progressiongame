package com.financegame.service;

import com.financegame.dto.StockDto;
import com.financegame.entity.Investment;
import com.financegame.entity.Stock;
import com.financegame.entity.StockPriceHistory;
import com.financegame.repository.InvestmentRepository;
import com.financegame.repository.StockPriceHistoryRepository;
import com.financegame.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceHistoryRepository historyRepository;
    private final InvestmentRepository investmentRepository;
    private final Random random = new Random();

    public StockService(StockRepository stockRepository,
                        StockPriceHistoryRepository historyRepository,
                        InvestmentRepository investmentRepository) {
        this.stockRepository = stockRepository;
        this.historyRepository = historyRepository;
        this.investmentRepository = investmentRepository;
    }

    @Transactional(readOnly = true)
    public List<StockDto> getAllStocks() {
        return stockRepository.findAll().stream()
            .map(s -> StockDto.from(s, historyRepository.findByStockIdOrderByTurnAsc(s.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public StockDto getByTicker(String ticker) {
        Stock s = stockRepository.findByTicker(ticker)
            .orElseThrow(() -> new RuntimeException("Aktie nicht gefunden: " + ticker));
        return StockDto.from(s, historyRepository.findByStockIdOrderByTurnAsc(s.getId()));
    }

    /**
     * Called once per turn by TurnService.
     * Applies random price movement and updates all open positions.
     */
    @Transactional
    public void simulatePrices(int currentTurn) {
        List<Stock> stocks = stockRepository.findAll();
        for (Stock stock : stocks) {
            BigDecimal newPrice = applyPriceMovement(stock);
            stock.setCurrentPrice(newPrice);
            stockRepository.save(stock);

            // Record history
            historyRepository.save(new StockPriceHistory(stock.getId(), newPrice, currentTurn));

            // Update open investment positions for this stock
            for (Investment inv : investmentRepository.findByStockId(stock.getId())) {
                inv.setCurrentValue(newPrice.multiply(inv.getQuantity()).setScale(2, RoundingMode.HALF_UP));
                investmentRepository.save(inv);
            }
        }
    }

    private BigDecimal applyPriceMovement(Stock stock) {
        double maxSwing = "MEME".equals(stock.getType()) ? 0.80 : 0.15;
        double change = (random.nextDouble() * 2 - 1) * maxSwing; // [-max, +max]
        BigDecimal factor = BigDecimal.valueOf(1 + change);
        return stock.getCurrentPrice()
            .multiply(factor)
            .max(BigDecimal.valueOf(0.01))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
