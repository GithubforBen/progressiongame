package com.financegame.service;

import com.financegame.config.GameConfig;
import com.financegame.dto.StockDto;
import com.financegame.entity.Investment;
import com.financegame.entity.Stock;
import com.financegame.entity.StockPriceHistory;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.InvestmentRepository;
import com.financegame.repository.StockPriceHistoryRepository;
import com.financegame.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceHistoryRepository historyRepository;
    private final InvestmentRepository investmentRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final GameConfig gameConfig;
    private final Random random = new Random();

    public StockService(StockRepository stockRepository,
                        StockPriceHistoryRepository historyRepository,
                        InvestmentRepository investmentRepository,
                        EducationProgressRepository educationProgressRepository,
                        GameConfig gameConfig) {
        this.stockRepository = stockRepository;
        this.historyRepository = historyRepository;
        this.investmentRepository = investmentRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.gameConfig = gameConfig;
    }

    @Transactional(readOnly = true)
    public List<StockDto> getAllStocks(Long playerId) {
        List<String> completedStages = getCompletedStages(playerId);
        return stockRepository.findAll().stream()
            .map(s -> StockDto.from(s, historyRepository.findByStockIdAndPlayerIdOrderByTurnAsc(s.getId(), playerId), completedStages))
            .toList();
    }

    @Transactional(readOnly = true)
    public StockDto getByTicker(String ticker, Long playerId) {
        Stock s = stockRepository.findByTicker(ticker)
            .orElseThrow(() -> new RuntimeException("Aktie nicht gefunden: " + ticker));
        List<String> completedStages = getCompletedStages(playerId);
        return StockDto.from(s, historyRepository.findByStockIdAndPlayerIdOrderByTurnAsc(s.getId(), playerId), completedStages);
    }

    /**
     * Called once per turn by TurnService. Simulates prices independently per player.
     */
    @Transactional
    public void simulatePrices(Long playerId, int currentTurn) {
        List<Stock> stocks = stockRepository.findAll();
        for (Stock stock : stocks) {
            if (historyRepository.existsByStockIdAndPlayerIdAndTurn(stock.getId(), playerId, currentTurn)) {
                continue;
            }

            BigDecimal currentPrice = historyRepository
                .findLatestPriceByStockIdAndPlayerId(stock.getId(), playerId)
                .orElse(stock.getCurrentPrice());

            BigDecimal newPrice = applyPriceMovement(currentPrice, stock.getType());
            historyRepository.save(new StockPriceHistory(stock.getId(), playerId, newPrice, currentTurn));

            for (Investment inv : investmentRepository.findByPlayerIdAndStockId(playerId, stock.getId())) {
                inv.setCurrentValue(newPrice.multiply(inv.getQuantity()).setScale(2, RoundingMode.HALF_UP));
                investmentRepository.save(inv);
            }
        }
    }

    private BigDecimal applyPriceMovement(BigDecimal currentPrice, String stockType) {
        double maxSwing = gameConfig.getStockVolatility().getOrDefault(stockType, 0.15);
        double change = (random.nextDouble() * 2 - 1) * maxSwing;
        BigDecimal factor = BigDecimal.valueOf(1 + change);
        return currentPrice
            .multiply(factor)
            .max(BigDecimal.valueOf(0.01))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> getCompletedStages(Long playerId) {
        return educationProgressRepository.findByPlayerId(playerId)
            .map(ep -> Arrays.asList(ep.getCompletedStages()))
            .orElse(List.of());
    }
}
