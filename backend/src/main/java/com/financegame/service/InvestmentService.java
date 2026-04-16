package com.financegame.service;

import com.financegame.dto.InvestmentDto;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.Investment;
import com.financegame.entity.Stock;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.InvestmentRepository;
import com.financegame.repository.StockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final StockRepository stockRepository;
    private final CharacterService characterService;
    private final EducationProgressRepository educationProgressRepository;

    public InvestmentService(InvestmentRepository investmentRepository,
                              StockRepository stockRepository,
                              CharacterService characterService,
                              EducationProgressRepository educationProgressRepository) {
        this.investmentRepository = investmentRepository;
        this.stockRepository = stockRepository;
        this.characterService = characterService;
        this.educationProgressRepository = educationProgressRepository;
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

        BigDecimal totalCost = stock.getCurrentPrice()
            .multiply(quantity)
            .setScale(2, RoundingMode.HALF_UP);

        // Cert-based access check
        if (stock.getRequiredCert() != null) {
            List<String> completedStages = educationProgressRepository.findByPlayerId(playerId)
                .map(ep -> Arrays.asList(ep.getCompletedStages()))
                .orElse(List.of());
            if (!completedStages.contains(stock.getRequiredCert())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Weiterbildung erforderlich: " + stock.getRequiredCert());
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

        // Update net worth
        recalcNetWorth(playerId);

        return InvestmentDto.from(investment);
    }

    @Transactional
    public void sellStock(Long playerId, Long investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Investment nicht gefunden"));

        if (!investment.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nicht dein Investment");
        }
        if (!"STOCK".equals(investment.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nur Aktien koennen so verkauft werden");
        }

        // Return current value to cash
        characterService.addCash(playerId, investment.getCurrentValue());
        investmentRepository.delete(investmentId);

        recalcNetWorth(playerId);
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
