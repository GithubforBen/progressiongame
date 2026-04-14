package com.financegame.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financegame.dto.BlackjackStateDto;
import com.financegame.dto.PokerResultDto;
import com.financegame.dto.SlotResultDto;
import com.financegame.entity.GamblingSession;
import com.financegame.repository.GamblingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GamblingService {

    private static final BigDecimal MIN_BET = new BigDecimal("1.00");
    private static final BigDecimal MAX_BET = new BigDecimal("10000.00");

    private final GamblingRepository gamblingRepository;
    private final CharacterService characterService;
    private final ObjectMapper objectMapper;

    public GamblingService(GamblingRepository gamblingRepository,
                           CharacterService characterService,
                           ObjectMapper objectMapper) {
        this.gamblingRepository = gamblingRepository;
        this.characterService = characterService;
        this.objectMapper = objectMapper;
    }

    // ======================== SLOTS ========================
    // House edge ~15% (EV: 0.01*50 + 0.02*10 + 0.05*5 + 0.10*1.5 = 0.85)

    @Transactional
    public SlotResultDto playSlots(Long playerId, BigDecimal bet) {
        validateBet(bet);
        characterService.deductCash(playerId, bet, "Slots");

        int roll = ThreadLocalRandom.current().nextInt(100);
        String outcome;
        BigDecimal multiplier;
        String[] reels;

        if (roll < 1) {                        // 1% – Jackpot
            outcome = "JACKPOT";
            multiplier = new BigDecimal("50");
            reels = new String[]{"SEVEN", "SEVEN", "SEVEN"};
        } else if (roll < 3) {                 // 2% – Großer Gewinn
            outcome = "BIG_WIN";
            multiplier = new BigDecimal("10");
            reels = new String[]{"BELL", "BELL", "BELL"};
        } else if (roll < 8) {                 // 5% – Gewinn
            outcome = "WIN";
            multiplier = new BigDecimal("5");
            String[] triple = {"BAR", "CHERRY", "LEMON"};
            String sym = triple[ThreadLocalRandom.current().nextInt(3)];
            reels = new String[]{sym, sym, sym};
        } else if (roll < 18) {                // 10% – Kleiner Gewinn
            outcome = "SMALL_WIN";
            multiplier = new BigDecimal("1.5");
            reels = generateTwoMatch();
        } else {                               // 82% – Verlust
            outcome = "LOSS";
            multiplier = BigDecimal.ZERO;
            reels = generateNoMatch();
        }

        BigDecimal payout = bet.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        if (payout.compareTo(BigDecimal.ZERO) > 0) {
            characterService.addCash(playerId, payout);
        }
        return new SlotResultDto(reels, outcome, bet, payout, payout.subtract(bet));
    }

    private String[] generateTwoMatch() {
        String[] symbols = {"CHERRY", "LEMON", "BAR", "BELL", "SEVEN"};
        Random rng = ThreadLocalRandom.current();
        String match = symbols[rng.nextInt(symbols.length)];
        String other;
        do { other = symbols[rng.nextInt(symbols.length)]; } while (other.equals(match));
        String[] reels = {match, match, match};
        reels[rng.nextInt(3)] = other;
        return reels;
    }

    private String[] generateNoMatch() {
        String[] symbols = {"CHERRY", "LEMON", "BAR", "BELL", "SEVEN", "BLANK"};
        Random rng = ThreadLocalRandom.current();
        String[] reels = new String[3];
        do {
            for (int i = 0; i < 3; i++) reels[i] = symbols[rng.nextInt(symbols.length)];
        } while (reels[0].equals(reels[1]) || reels[1].equals(reels[2]) || reels[0].equals(reels[2]));
        return reels;
    }

    // ======================== BLACKJACK ========================
    // Standard rules: dealer hits soft 17. Blackjack pays 2.5x (1.5:1 profit).

    @Transactional
    public BlackjackStateDto startBlackjack(Long playerId, BigDecimal bet) {
        validateBet(bet);
        characterService.deductCash(playerId, bet, "Blackjack");

        List<String> deck = buildShuffledDeck();
        List<String> playerHand = new ArrayList<>();
        List<String> dealerHand = new ArrayList<>();

        playerHand.add(deck.remove(0));
        dealerHand.add(deck.remove(0));
        playerHand.add(deck.remove(0));
        dealerHand.add(deck.remove(0));

        BlackjackState state = new BlackjackState(deck, playerHand, dealerHand);
        GamblingSession session = new GamblingSession();
        session.setPlayerId(playerId);
        session.setGameType("BLACKJACK");
        session.setStatus("IN_PROGRESS");
        session.setBetAmount(bet);
        session.setPayoutAmount(BigDecimal.ZERO);
        session.setGameState(serializeState(state));
        gamblingRepository.save(session);

        // Auto-resolve on natural blackjack
        if (calcBjTotal(playerHand) == 21) {
            return resolveStand(session, state, playerId);
        }
        return toBlackjackDto(session, playerHand, dealerHand, true);
    }

    @Transactional
    public BlackjackStateDto hitBlackjack(Long playerId, Long sessionId) {
        GamblingSession session = getActiveSession(playerId, sessionId);
        BlackjackState state = deserializeState(session.getGameState());

        state.playerHand.add(state.deck.remove(0));
        int total = calcBjTotal(state.playerHand);

        if (total > 21) {
            session.setStatus("LOST");
            session.setPayoutAmount(BigDecimal.ZERO);
            session.setGameState(serializeState(state));
            gamblingRepository.save(session);
            return toBlackjackDto(session, state.playerHand, state.dealerHand, false);
        }

        session.setGameState(serializeState(state));
        gamblingRepository.save(session);
        return toBlackjackDto(session, state.playerHand, state.dealerHand, true);
    }

    @Transactional
    public BlackjackStateDto standBlackjack(Long playerId, Long sessionId) {
        GamblingSession session = getActiveSession(playerId, sessionId);
        BlackjackState state = deserializeState(session.getGameState());
        return resolveStand(session, state, playerId);
    }

    private BlackjackStateDto resolveStand(GamblingSession session, BlackjackState state, Long playerId) {
        while (calcBjTotal(state.dealerHand) < 17) {
            state.dealerHand.add(state.deck.remove(0));
        }

        int playerTotal = calcBjTotal(state.playerHand);
        int dealerTotal = calcBjTotal(state.dealerHand);
        boolean isNaturalBlackjack = playerTotal == 21 && state.playerHand.size() == 2;

        BigDecimal payout;
        String result;
        if (dealerTotal > 21 || playerTotal > dealerTotal) {
            payout = isNaturalBlackjack
                ? session.getBetAmount().multiply(new BigDecimal("2.5")).setScale(2, RoundingMode.HALF_UP)
                : session.getBetAmount().multiply(BigDecimal.TWO);
            result = "WON";
        } else if (playerTotal == dealerTotal) {
            payout = session.getBetAmount();
            result = "PUSH";
        } else {
            payout = BigDecimal.ZERO;
            result = "LOST";
        }

        session.setStatus(result);
        session.setPayoutAmount(payout);
        session.setGameState(serializeState(state));
        gamblingRepository.save(session);

        if (payout.compareTo(BigDecimal.ZERO) > 0) {
            characterService.addCash(playerId, payout);
        }
        return toBlackjackDto(session, state.playerHand, state.dealerHand, false);
    }

    private GamblingSession getActiveSession(Long playerId, Long sessionId) {
        GamblingSession session = gamblingRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sitzung nicht gefunden"));
        if (!session.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kein Zugriff");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spiel bereits beendet");
        }
        return session;
    }

    private BlackjackStateDto toBlackjackDto(GamblingSession session,
                                              List<String> playerHand,
                                              List<String> dealerHand,
                                              boolean hideSecondDealerCard) {
        List<String> visibleDealer = hideSecondDealerCard
            ? List.of(dealerHand.get(0), "??")
            : new ArrayList<>(dealerHand);
        int playerTotal = calcBjTotal(playerHand);
        int dealerVisible = hideSecondDealerCard
            ? singleCardValue(dealerHand.get(0))
            : calcBjTotal(dealerHand);

        return new BlackjackStateDto(
            session.getId(),
            new ArrayList<>(playerHand),
            visibleDealer,
            playerTotal,
            dealerVisible,
            session.getStatus(),
            session.getBetAmount(),
            session.getPayoutAmount(),
            session.getPayoutAmount().subtract(session.getBetAmount())
        );
    }

    private String serializeState(BlackjackState state) {
        try { return objectMapper.writeValueAsString(state); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    private BlackjackState deserializeState(String json) {
        try { return objectMapper.readValue(json, BlackjackState.class); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    // ======================== POKER ========================
    // Simplified 5-card draw vs AI. House rake 5% on wins.

    @Transactional
    public PokerResultDto playPoker(Long playerId, BigDecimal bet) {
        validateBet(bet);
        characterService.deductCash(playerId, bet, "Poker");

        List<String> deck = buildShuffledDeck();
        List<String> playerCards = new ArrayList<>(deck.subList(0, 5));
        List<String> aiCards = new ArrayList<>(deck.subList(5, 10));

        int playerRank = evaluatePokerHand(playerCards);
        int aiRank = evaluatePokerHand(aiCards);

        BigDecimal payout;
        String result;
        if (playerRank > aiRank) {
            // Win: 5% rake → total return = bet * 1.95
            payout = bet.multiply(new BigDecimal("1.95")).setScale(2, RoundingMode.HALF_UP);
            result = "WON";
        } else if (playerRank == aiRank) {
            payout = bet;
            result = "PUSH";
        } else {
            payout = BigDecimal.ZERO;
            result = "LOST";
        }

        if (payout.compareTo(BigDecimal.ZERO) > 0) {
            characterService.addCash(playerId, payout);
        }

        return new PokerResultDto(
            playerCards,
            aiCards,
            pokerHandName(playerRank),
            pokerHandName(aiRank),
            result,
            bet,
            payout,
            payout.subtract(bet)
        );
    }

    // ======================== HELPERS ========================

    private void validateBet(BigDecimal bet) {
        if (bet == null || bet.compareTo(MIN_BET) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mindesteinsatz ist 1 €");
        }
        if (bet.compareTo(MAX_BET) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximaleinsatz ist 10.000 €");
        }
    }

    private List<String> buildShuffledDeck() {
        String[] ranks = {"2","3","4","5","6","7","8","9","T","J","Q","K","A"};
        String[] suits = {"H","D","C","S"};
        List<String> deck = new ArrayList<>(52);
        for (String r : ranks) for (String s : suits) deck.add(r + s);
        Collections.shuffle(deck, new Random(ThreadLocalRandom.current().nextLong()));
        return deck;
    }

    int calcBjTotal(List<String> hand) {
        int total = 0;
        int aces = 0;
        for (String card : hand) {
            int v = singleCardValue(card);
            if (v == 11) aces++;
            total += v;
        }
        while (total > 21 && aces > 0) { total -= 10; aces--; }
        return total;
    }

    private int singleCardValue(String card) {
        return switch (card.charAt(0)) {
            case '2' -> 2; case '3' -> 3; case '4' -> 4; case '5' -> 5;
            case '6' -> 6; case '7' -> 7; case '8' -> 8; case '9' -> 9;
            case 'T', 'J', 'Q', 'K' -> 10;
            case 'A' -> 11;
            default -> 0;
        };
    }

    private int cardRank(char c) {
        return switch (c) {
            case '2' -> 2; case '3' -> 3; case '4' -> 4; case '5' -> 5;
            case '6' -> 6; case '7' -> 7; case '8' -> 8; case '9' -> 9;
            case 'T' -> 10; case 'J' -> 11; case 'Q' -> 12; case 'K' -> 13; case 'A' -> 14;
            default -> 0;
        };
    }

    int evaluatePokerHand(List<String> cards) {
        int[] ranks = cards.stream().mapToInt(c -> cardRank(c.charAt(0))).sorted().toArray();
        Map<Character, Long> suitFreq = cards.stream()
            .collect(Collectors.groupingBy(c -> c.charAt(1), Collectors.counting()));
        Map<Integer, Long> rankFreq = IntStream.of(ranks).boxed()
            .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

        boolean isFlush = suitFreq.values().stream().anyMatch(v -> v == 5);
        boolean isStraight = (ranks[4] - ranks[0] == 4 && rankFreq.size() == 5)
            // Ace-low straight: A-2-3-4-5
            || (ranks[4] == 14 && ranks[3] == 5 && ranks[2] == 4 && ranks[1] == 3 && ranks[0] == 2);

        long pairs = rankFreq.values().stream().filter(v -> v == 2).count();
        boolean hasThree = rankFreq.values().stream().anyMatch(v -> v == 3);
        boolean hasFour  = rankFreq.values().stream().anyMatch(v -> v == 4);

        if (isFlush && isStraight && ranks[4] == 14 && ranks[3] == 13) return 9; // Royal Flush
        if (isFlush && isStraight) return 8;  // Straight Flush
        if (hasFour) return 7;                // Vierling
        if (hasThree && pairs == 1) return 6; // Full House
        if (isFlush) return 5;
        if (isStraight) return 4;
        if (hasThree) return 3;               // Drilling
        if (pairs == 2) return 2;             // Zwei Paare
        if (pairs == 1) return 1;             // Ein Paar
        return 0;                             // Highcard
    }

    private String pokerHandName(int rank) {
        return switch (rank) {
            case 9 -> "Royal Flush";
            case 8 -> "Straight Flush";
            case 7 -> "Vierling";
            case 6 -> "Full House";
            case 5 -> "Flush";
            case 4 -> "Straight";
            case 3 -> "Drilling";
            case 2 -> "Zwei Paare";
            case 1 -> "Ein Paar";
            default -> "Highcard";
        };
    }

    // ======================== INNER STATE CLASS ========================

    public static class BlackjackState {
        public List<String> deck;
        public List<String> playerHand;
        public List<String> dealerHand;

        public BlackjackState() {} // required for Jackson

        public BlackjackState(List<String> deck, List<String> playerHand, List<String> dealerHand) {
            this.deck = deck;
            this.playerHand = playerHand;
            this.dealerHand = dealerHand;
        }
    }
}
