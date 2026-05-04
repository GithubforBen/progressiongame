package com.financegame.service;

import com.financegame.domain.GameContext;
import com.financegame.domain.GameContextFactory;
import com.financegame.domain.condition.HasCertCondition;
import com.financegame.domain.events.StockPurchasedEvent;
import com.financegame.domain.events.StockSoldEvent;
import com.financegame.dto.InvestmentDto;
import com.financegame.dto.SellStockResponse;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.Investment;
import com.financegame.entity.Stock;
import com.financegame.repository.InvestmentRepository;
import com.financegame.repository.PlayerDelistedStockRepository;
import com.financegame.repository.StockPriceHistoryRepository;
import com.financegame.repository.StockRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final StockRepository stockRepository;
    private final StockPriceHistoryRepository historyRepository;
    private final PlayerDelistedStockRepository delistedStockRepository;
    private final CharacterService characterService;
    private final GameContextFactory gameContextFactory;
    private final ApplicationEventPublisher eventPublisher;

    public InvestmentService(InvestmentRepository investmentRepository,
                              StockRepository stockRepository,
                              StockPriceHistoryRepository historyRepository,
                              PlayerDelistedStockRepository delistedStockRepository,
                              CharacterService characterService,
                              GameContextFactory gameContextFactory,
                              ApplicationEventPublisher eventPublisher) {
        this.investmentRepository = investmentRepository;
        this.stockRepository = stockRepository;
        this.historyRepository = historyRepository;
        this.delistedStockRepository = delistedStockRepository;
        this.characterService = characterService;
        this.gameContextFactory = gameContextFactory;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<InvestmentDto> getPortfolio(Long playerId) {
        return investmentRepository.findByPlayerId(playerId)
            .stream()
            .map(InvestmentDto::from)
            .toList();
    }

    @Transactional
    public InvestmentDto buyStock(Long playerId, String ticker, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menge muss groesser als 0 sein");
        }

        Stock stock = stockRepository.findByTicker(ticker)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Aktie nicht gefunden: " + ticker));

        BigDecimal price = historyRepository
            .findLatestPriceByStockIdAndPlayerId(stock.getId(), playerId)
            .orElse(stock.getCurrentPrice());
        BigDecimal totalCost = price
            .multiply(quantity)
            .setScale(2, RoundingMode.HALF_UP);

        // Delisting guard — reject buy if stock is currently delisted for this player
        if (delistedStockRepository.isDelisted(playerId, stock.getId())) {
            throw new ResponseStatusException(HttpStatus.GONE,
                stock.getTicker() + " ist derzeit vom Markt genommen und kann nicht gekauft werden.");
        }

        // Cert-based access check
        if (stock.getRequiredCert() != null) {
            GameContext ctx = gameContextFactory.build(playerId);
            HasCertCondition certCheck = new HasCertCondition(stock.getRequiredCert());
            if (!certCheck.isMet(ctx)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, certCheck.describe());
            }
        }

        // Deduct cash (throws 400 if insufficient)
        characterService.deductCash(playerId, totalCost, "Aktie " + ticker);

        GameCharacter character = characterService.findOrThrow(playerId);

        Investment investment = new Investment();
        investment.setPlayerId(playerId);
        investment.setType("STOCK");
        investment.setName(stock.getName());
        investment.setStockId(stock.getId());
        investment.setQuantity(quantity);
        investment.setAmountInvested(totalCost);
        investment.setCurrentValue(totalCost);
        investment.setAcquiredAtTurn(character.getCurrentTurn());
        investmentRepository.save(investment);

        recalcNetWorth(playerId);

        eventPublisher.publishEvent(new StockPurchasedEvent(playerId, ticker, quantity, totalCost));

        return InvestmentDto.from(investment);
    }

    private static final BigDecimal TAX_RATE = new BigDecimal("0.25");

    @Transactional
    public SellStockResponse sellStock(Long playerId, Long investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Investment nicht gefunden"));

        if (!investment.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nicht dein Investment");
        }
        if (!"STOCK".equals(investment.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nur Aktien koennen so verkauft werden");
        }

        BigDecimal proceeds = investment.getCurrentValue();
        BigDecimal costBasis = investment.getAmountInvested();
        BigDecimal grossProfit = proceeds.subtract(costBasis);
        String ticker = investment.getName();

        BigDecimal taxPaid = BigDecimal.ZERO;
        if (grossProfit.compareTo(BigDecimal.ZERO) > 0) {
            taxPaid = grossProfit.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal netProceeds = proceeds.subtract(taxPaid);

        characterService.addCash(playerId, netProceeds);
        investmentRepository.delete(investmentId);

        recalcNetWorth(playerId);

        eventPublisher.publishEvent(new StockSoldEvent(playerId, ticker, proceeds, grossProfit));

        return new SellStockResponse(ticker, proceeds, costBasis, grossProfit, taxPaid, netProceeds);
    }

    private void recalcNetWorth(Long playerId) {
        BigDecimal totalInvestmentValue = investmentRepository.findByPlayerId(playerId)
            .stream()
            .map(Investment::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        GameCharacter character = characterService.findOrThrow(playerId);
        character.setNetWorth(character.getCash().add(totalInvestmentValue));
    }
}
