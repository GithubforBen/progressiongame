package com.financegame.service;

import com.financegame.domain.events.PlayerRegisteredEvent;
import com.financegame.dto.AuthResponse;
import com.financegame.dto.LoginRequest;
import com.financegame.dto.RegisterRequest;
import com.financegame.entity.EducationProgress;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.MonthlyExpense;
import com.financegame.entity.Player;
import com.financegame.entity.PlayerTravel;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.EducationProgressRepository;
import com.financegame.repository.MonthlyExpenseRepository;
import com.financegame.repository.PlayerRepository;
import com.financegame.repository.PlayerTravelRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final MonthlyExpenseRepository monthlyExpenseRepository;
    private final PlayerTravelRepository playerTravelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(
        PlayerRepository playerRepository,
        CharacterRepository characterRepository,
        EducationProgressRepository educationProgressRepository,
        MonthlyExpenseRepository monthlyExpenseRepository,
        PlayerTravelRepository playerTravelRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.playerRepository = playerRepository;
        this.characterRepository = characterRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.monthlyExpenseRepository = monthlyExpenseRepository;
        this.playerTravelRepository = playerTravelRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (playerRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Benutzername bereits vergeben");
        }

        Player player = new Player();
        player.setUsername(request.username());
        player.setPasswordHash(passwordEncoder.encode(request.password()));
        playerRepository.save(player);

        initCharacter(player.getId());
        initEducation(player.getId());
        initDefaultExpenses(player.getId());
        initTravel(player.getId());

        String token = jwtService.generateToken(player.getUsername(), player.getId());

        eventPublisher.publishEvent(new PlayerRegisteredEvent(player.getId(), player.getUsername()));

        return new AuthResponse(token, new AuthResponse.UserDto(player.getId(), player.getUsername()));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Player player = playerRepository.findByUsername(request.username())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Ungueltige Anmeldedaten"));

        if (!passwordEncoder.matches(request.password(), player.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Ungueltige Anmeldedaten");
        }

        String token = jwtService.generateToken(player.getUsername(), player.getId());
        return new AuthResponse(token, new AuthResponse.UserDto(player.getId(), player.getUsername()));
    }

    private void initCharacter(Long playerId) {
        GameCharacter character = new GameCharacter();
        character.setPlayerId(playerId);
        characterRepository.save(character);
    }

    private void initEducation(Long playerId) {
        EducationProgress ep = new EducationProgress();
        ep.setPlayerId(playerId);
        educationProgressRepository.save(ep);
    }

    private void initDefaultExpenses(Long playerId) {
        // Mandatory expenses every player starts with
        createExpense(playerId, "MIETE",              "Miete",              new BigDecimal("500.00"),  true,  true);
        createExpense(playerId, "ESSEN",              "Lebensmittel",       new BigDecimal("200.00"),  true,  true);
        createExpense(playerId, "KRANKENVERSICHERUNG","Krankenversicherung", new BigDecimal("250.00"),  true,  false);
    }

    private void initTravel(Long playerId) {
        playerTravelRepository.save(new PlayerTravel(playerId));
    }

    private void createExpense(Long playerId, String category, String label,
                               BigDecimal amount, boolean active, boolean mandatory) {
        MonthlyExpense expense = new MonthlyExpense();
        expense.setPlayerId(playerId);
        expense.setCategory(category);
        expense.setLabel(label);
        expense.setAmount(amount);
        expense.setActive(active);
        expense.setMandatory(mandatory);
        monthlyExpenseRepository.save(expense);
    }
}
