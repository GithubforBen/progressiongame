package com.financegame.service;

import com.financegame.config.GameConfig;
import com.financegame.domain.effect.EffectType;
import com.financegame.dto.StockDto;
import com.financegame.entity.EventLog;
import com.financegame.entity.Investment;
import com.financegame.entity.PlayerDelistedStock;
import com.financegame.entity.Stock;
import com.financegame.entity.StockPriceHistory;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.EventLogRepository;
import com.financegame.repository.InvestmentRepository;
import com.financegame.repository.PlayerDelistedStockRepository;
import com.financegame.repository.StockPriceHistoryRepository;
import com.financegame.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceHistoryRepository historyRepository;
    private final InvestmentRepository investmentRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final PlayerDelistedStockRepository delistedStockRepository;
    private final EventLogRepository eventLogRepository;
    private final GameConfig gameConfig;
    private final PlayerEffectsService playerEffectsService;
    private final Random random = new Random();

    public StockService(StockRepository stockRepository,
                        StockPriceHistoryRepository historyRepository,
                        InvestmentRepository investmentRepository,
                        EducationProgressRepository educationProgressRepository,
                        PlayerDelistedStockRepository delistedStockRepository,
                        EventLogRepository eventLogRepository,
                        GameConfig gameConfig,
                        PlayerEffectsService playerEffectsService) {
        this.stockRepository = stockRepository;
        this.historyRepository = historyRepository;
        this.investmentRepository = investmentRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.delistedStockRepository = delistedStockRepository;
        this.eventLogRepository = eventLogRepository;
        this.gameConfig = gameConfig;
        this.playerEffectsService = playerEffectsService;
    }

    @Transactional(readOnly = true)
    public List<StockDto> getAllStocks(Long playerId) {
        List<String> completedStages = getCompletedStages(playerId);
        Set<Long> delistedIds = delistedStockRepository.findDelistedStockIds(playerId);
        return stockRepository.findAll().stream()
            .map(s -> StockDto.from(
                s,
                historyRepository.findByStockIdAndPlayerIdOrderByTurnAsc(s.getId(), playerId),
                completedStages,
                delistedIds.contains(s.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public StockDto getByTicker(String ticker, Long playerId) {
        Stock s = stockRepository.findByTicker(ticker)
            .orElseThrow(() -> new RuntimeException("Aktie nicht gefunden: " + ticker));
        List<String> completedStages = getCompletedStages(playerId);
        boolean delisted = delistedStockRepository.isDelisted(playerId, s.getId());
        return StockDto.from(s, historyRepository.findByStockIdAndPlayerIdOrderByTurnAsc(s.getId(), playerId), completedStages, delisted);
    }

    /**
     * Called once per turn by TurnService. Simulates prices independently per player.
     * Uses Ornstein-Uhlenbeck mean-reversion in log space. Stocks that fall below
     * their delisting threshold are bankrupted for this player; they relist at initial
     * price after a random delay.
     */
    @Transactional
    public void simulatePrices(Long playerId, int currentTurn) {
        List<Stock> stocks = stockRepository.findAll();
        GameConfig.StockDelistingConfig delistCfg = gameConfig.getStockDelisting();

        // Step 1: Check relist eligibility for all currently delisted stocks
        List<PlayerDelistedStock> delistedList = delistedStockRepository.findAllForPlayer(playerId);
        Set<Long> delistedIds = new HashSet<>(delistedStockRepository.findDelistedStockIds(playerId));

        for (PlayerDelistedStock ds : delistedList) {
            int minRelistTurn = ds.getDelistedAtTurn() + delistCfg.getMinDelistTurns();
            if (currentTurn >= minRelistTurn && random.nextDouble() < delistCfg.getRelistChancePerTurn()) {
                stocks.stream()
                    .filter(s -> s.getId().equals(ds.getStockId()))
                    .findFirst()
                    .ifPresent(s -> {
                        relistStock(s, playerId, currentTurn);
                        delistedIds.remove(s.getId());
                    });
            }
        }

        // Step 2: Simulate price for each active (non-delisted) stock
        double volatilityReduction = playerEffectsService.getEffects(playerId).get(EffectType.STOCK_VOLATILITY_REDUCTION);
        for (Stock stock : stocks) {
            if (delistedIds.contains(stock.getId())) continue;
            if (historyRepository.existsByStockIdAndPlayerIdAndTurn(stock.getId(), playerId, currentTurn)) continue;

            BigDecimal currentPrice = historyRepository
                .findLatestPriceByStockIdAndPlayerId(stock.getId(), playerId)
                .orElse(stock.getInitialPrice());

            BigDecimal newPrice = applyPriceMovement(currentPrice, stock.getInitialPrice(), stock.getType(), volatilityReduction);

            if (newPrice == null) {
                delistStock(stock, playerId, currentTurn);
            } else {
                historyRepository.save(new StockPriceHistory(stock.getId(), playerId, newPrice, currentTurn));
                updateInvestmentValues(playerId, stock.getId(), newPrice);
            }
        }
    }

    /**
     * Ornstein-Uhlenbeck mean-reversion in log space:
     *   logReturn = θ * ln(P_initial / P_current) + ε,   ε ~ Uniform(-σ, σ)
     *   P_new = P_current * exp(logReturn)
     *
     * Returns null when the computed price falls below the delisting threshold,
     * signalling that the stock should be bankrupted for this player.
     *
     * Delisting threshold = max(0.01, initialPrice * thresholdFraction)
     * — the 0.01 floor removes the exploit where a penny stock is protected by
     *   the minimum storable price and asymmetrically drifts upward.
     */
    private BigDecimal applyPriceMovement(BigDecimal currentPrice, BigDecimal initialPrice, String type, double volatilityReduction) {
        double theta = gameConfig.getStockReversionSpeed().getOrDefault(type, 0.08);
        double sigma = gameConfig.getStockVolatility().getOrDefault(type, 0.15)
            * (1.0 - Math.min(0.5, volatilityReduction));

        double logRatio = Math.log(initialPrice.doubleValue() / currentPrice.doubleValue());
        double drift = theta * logRatio;
        double noise = (random.nextDouble() * 2 - 1) * sigma;
        double logReturn = drift + noise;

        BigDecimal rawPrice = currentPrice.multiply(BigDecimal.valueOf(Math.exp(logReturn)));

        // Effective delisting threshold: the higher of storage floor (0.01) and the % of initial.
        // Using max(0.01, ...) means even a penny stock at its initial price can be delisted if
        // the OU model computes a price below a cent, preventing the floor-bounce exploit.
        BigDecimal delistThreshold = initialPrice
            .multiply(BigDecimal.valueOf(gameConfig.getStockDelisting().getThresholdFraction()))
            .max(BigDecimal.valueOf(0.01));

        if (rawPrice.compareTo(delistThreshold) < 0) {
            return null; // delist signal
        }

        return rawPrice.max(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
    }

    private void delistStock(Stock stock, Long playerId, int currentTurn) {
        List<Investment> investments = investmentRepository.findByPlayerIdAndStockId(playerId, stock.getId());
        BigDecimal totalLost = investments.stream()
            .map(Investment::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        investments.forEach(inv -> investmentRepository.delete(inv.getId()));

        delistedStockRepository.save(new PlayerDelistedStock(playerId, stock.getId(), currentTurn));

        String description = String.format(
            "%s (%s) wurde insolvent und vom Markt genommen! Deine Investition (%.2f €) ist vollständig verloren.",
            stock.getName(), stock.getTicker(), totalLost);
        eventLogRepository.save(new EventLog(playerId, description, totalLost.negate(), "STOCK_DELISTED", currentTurn));
    }

    private void relistStock(Stock stock, Long playerId, int currentTurn) {
        delistedStockRepository.delete(playerId, stock.getId());

        // Preserve crash history but add a fresh entry at initial price so the chart shows the relisting
        historyRepository.save(new StockPriceHistory(stock.getId(), playerId, stock.getInitialPrice(), currentTurn));

        String description = String.format(
            "%s (%s) wurde wieder an der Börse notiert (Startpreis: %.2f €).",
            stock.getName(), stock.getTicker(), stock.getInitialPrice());
        eventLogRepository.save(new EventLog(playerId, description, null, "STOCK_RELISTED", currentTurn));
    }

    private void updateInvestmentValues(Long playerId, Long stockId, BigDecimal newPrice) {
        for (Investment inv : investmentRepository.findByPlayerIdAndStockId(playerId, stockId)) {
            inv.setCurrentValue(newPrice.multiply(inv.getQuantity()).setScale(2, RoundingMode.HALF_UP));
            investmentRepository.save(inv);
        }
    }

    private List<String> getCompletedStages(Long playerId) {
        return educationProgressRepository.findByPlayerId(playerId)
            .map(ep -> Arrays.asList(ep.getCompletedStages()))
            .orElse(List.of());
    }
}
