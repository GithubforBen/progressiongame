package com.financegame.service;

import com.financegame.config.GameConfig;
import com.financegame.dto.CharacterDto;
import com.financegame.entity.GameCharacter;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.EducationProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class TaxEvasionService {

    private final CharacterService characterService;
    private final CharacterRepository characterRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final GameConfig gameConfig;

    public TaxEvasionService(
        CharacterService characterService,
        CharacterRepository characterRepository,
        EducationProgressRepository educationProgressRepository,
        GameConfig gameConfig
    ) {
        this.characterService = characterService;
        this.characterRepository = characterRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.gameConfig = gameConfig;
    }

    public record StatusDto(
        boolean active,
        int level,
        BigDecimal cumulativeEvaded,
        double detectionChancePercent,
        int jailMonthsRemaining,
        int exileMonthsRemaining,
        boolean caughtPending,
        BigDecimal bailAmount,
        int finanzamtAuditMonthsRemaining
    ) {}

    public StatusDto getStatus(Long playerId) {
        GameCharacter c = characterService.findOrThrow(playerId);
        int level = evasionLevel(playerId);
        BigDecimal bail = bailAmount(c.getCumulativeEvadedTaxes());
        return new StatusDto(
            c.isTaxEvasionActive(),
            level,
            c.getCumulativeEvadedTaxes(),
            detectionChance(level) * 100,
            c.getJailMonthsRemaining(),
            c.getExileMonthsRemaining(),
            c.isTaxEvasionCaughtPending(),
            bail,
            c.getFinanzamtAuditMonthsRemaining()
        );
    }

    @Transactional
    public CharacterDto toggle(Long playerId) {
        GameCharacter c = characterService.findOrThrow(playerId);
        if (evasionLevel(playerId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Kein Steuerhinterziehungs-Level freigeschaltet.");
        }
        if (c.isTaxEvasionCaughtPending()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Entscheide dich zuerst über dein Schicksal.");
        }
        if (c.getJailMonthsRemaining() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Im Gefängnis kannst du keine Steuern hinterziehen.");
        }
        if (!c.isTaxEvasionActive() && c.getFinanzamtAuditMonthsRemaining() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Steuerhinterziehung gesperrt: Finanzamt-Überprüfung läuft noch "
                + c.getFinanzamtAuditMonthsRemaining() + " Monate.");
        }
        c.setTaxEvasionActive(!c.isTaxEvasionActive());
        characterRepository.save(c);
        return CharacterDto.from(c);
    }

    @Transactional
    public CharacterDto resolveCaught(Long playerId, String choice) {
        GameCharacter c = characterService.findOrThrow(playerId);
        if (!c.isTaxEvasionCaughtPending()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Keine ausstehende Steuerfahndung.");
        }

        if ("JAIL".equalsIgnoreCase(choice)) {
            c.setJailMonthsRemaining(6);
            c.setSchufaScore(LoanService.clampSchufa(c.getSchufaScore() - 100));
        } else if ("FLEE".equalsIgnoreCase(choice)) {
            BigDecimal bail = bailAmount(c.getCumulativeEvadedTaxes());
            if (c.getCash().compareTo(bail) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nicht genug Geld für die Kaution (" + bail + " €).");
            }
            c.setCash(c.getCash().subtract(bail));
            c.setExileMonthsRemaining(3);
            c.setSchufaScore(LoanService.clampSchufa(c.getSchufaScore() - 50));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültige Wahl: JAIL oder FLEE.");
        }

        c.setTaxEvasionCaughtPending(false);
        c.setCumulativeEvadedTaxes(BigDecimal.ZERO);
        characterRepository.save(c);
        return CharacterDto.from(c);
    }

    private int evasionLevel(Long playerId) {
        var ep = educationProgressRepository.findByPlayerId(playerId).orElse(null);
        if (ep == null) return 0;
        List<String> completed = Arrays.asList(ep.getCompletedStages());
        List<GameConfig.TaxEvasionConfig.TaxEvasionLevel> levels = gameConfig.getTaxEvasion().getLevels();
        for (int i = levels.size() - 1; i >= 0; i--) {
            String certKey = "WEITERBILDUNG_" + levels.get(i).getCertSuffix();
            if (completed.contains(certKey)) return i + 1;
        }
        return 0;
    }

    private double detectionChance(int level) {
        List<GameConfig.TaxEvasionConfig.TaxEvasionLevel> levels = gameConfig.getTaxEvasion().getLevels();
        if (level < 1 || level > levels.size()) return 1.0;
        return levels.get(level - 1).getDetectionChance();
    }

    static BigDecimal bailAmount(BigDecimal cumulativeEvaded) {
        BigDecimal triple = cumulativeEvaded.multiply(BigDecimal.valueOf(3));
        return triple.max(BigDecimal.valueOf(5000)).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
