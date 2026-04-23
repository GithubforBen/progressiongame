package com.financegame.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financegame.dto.BlackjackStateDto;
import com.financegame.dto.PokerResultDto;
import com.financegame.dto.SlotResultDto;
import com.financegame.dto.TexasHoldemStateDto;
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

    // ======================== TEXAS HOLD'EM ========================
    // 2-phase betting per street: player acts → bots act → if bots raised → player responds once

    @Transactional
    public TexasHoldemStateDto startTexasHoldem(Long playerId, BigDecimal bet) {
        validateBet(bet);
        characterService.deductCash(playerId, bet, "Texas Hold'em");

        List<String> deck = buildShuffledDeck();
        List<String> playerCards = new ArrayList<>(List.of(deck.remove(0), deck.remove(0)));
        List<List<String>> botCards = new ArrayList<>();
        for (int i = 0; i < 4; i++) botCards.add(new ArrayList<>(List.of(deck.remove(0), deck.remove(0))));

        // Randomly assign difficulties: always 2× EASY, 1× MEDIUM, 1× HARD
        List<String> diffPool = new ArrayList<>(List.of("EASY", "EASY", "MEDIUM", "HARD"));
        Collections.shuffle(diffPool, new Random(ThreadLocalRandom.current().nextLong()));

        // Pick a personality label per difficulty (not revealing the level)
        Random rng = ThreadLocalRandom.current();
        List<String> personalities = new ArrayList<>();
        String[] easyNames  = {"Draufgänger", "Glücksspieler", "Risikofreak"};
        String[] medNames   = {"Stratege", "Bedächtiger", "Kalkulierer"};
        String[] hardNames  = {"Profi", "Haifisch", "Eiskalter"};
        for (String d : diffPool) {
            personalities.add(switch (d) {
                case "EASY" -> easyNames[rng.nextInt(easyNames.length)];
                case "HARD" -> hardNames[rng.nextInt(hardNames.length)];
                default     -> medNames[rng.nextInt(medNames.length)];
            });
        }

        // One random bot is "stubborn" — will always call, never fold, regardless of raises
        int stubbornIndex = rng.nextInt(4);
        List<Boolean> stubborn = new ArrayList<>(List.of(false, false, false, false));
        stubborn.set(stubbornIndex, true);

        TexasHoldemState state = new TexasHoldemState();
        state.deck = deck;
        state.playerCards = playerCards;
        state.botCards = botCards;
        state.communityCards = new ArrayList<>();
        state.pot = bet.multiply(BigDecimal.valueOf(5));
        state.playerStake = bet;
        state.initialBet = bet;
        state.botFolded = new ArrayList<>(List.of(false, false, false, false));
        state.street = "PREFLOP";
        state.botDifficulties = diffPool;
        state.botPersonalities = personalities;
        state.botStubborn = stubborn;
        state.currentStreetBet = BigDecimal.ZERO;
        state.playerStreetBet = BigDecimal.ZERO;
        state.botStreetBets = new ArrayList<>(List.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        state.raisesThisStreet = 0;
        state.awaitingPlayerResponse = false;

        GamblingSession session = new GamblingSession();
        session.setPlayerId(playerId);
        session.setGameType("TEXAS_HOLDEM");
        session.setStatus("IN_PROGRESS");
        session.setBetAmount(bet);
        session.setPayoutAmount(BigDecimal.ZERO);
        session.setGameState(serializeTHState(state));
        gamblingRepository.save(session);

        return buildTHDto(session, state, List.of());
    }

    @Transactional
    public TexasHoldemStateDto actTexasHoldem(Long playerId, Long sessionId, String action, BigDecimal customAmount) {
        GamblingSession session = getActiveSession(playerId, sessionId);
        TexasHoldemState state = deserializeTHState(session.getGameState());
        initMissingTHFields(state);

        boolean wasAwaiting = state.awaitingPlayerResponse;
        state.awaitingPlayerResponse = false;

        List<TexasHoldemStateDto.ActionEntry> log = new ArrayList<>();

        // ── Player action ──────────────────────────────────────────────
        if ("FOLD".equals(action)) {
            session.setStatus("LOST");
            session.setPayoutAmount(BigDecimal.ZERO);
            session.setGameState(serializeTHState(state));
            gamblingRepository.save(session);
            return buildTHDtoFinal(session, state, "LOST", BigDecimal.ZERO, List.of());
        }

        BigDecimal playerOwed = state.currentStreetBet.subtract(state.playerStreetBet).max(BigDecimal.ZERO);

        if ("RAISE".equals(action)) {
            // raiseBy = the amount raised above the call; minimum = initialBet
            BigDecimal raiseBy = (customAmount != null && customAmount.compareTo(state.initialBet) >= 0)
                ? customAmount.setScale(2, RoundingMode.HALF_UP)
                : state.initialBet;
            BigDecimal cost = playerOwed.add(raiseBy);
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                characterService.deductCash(playerId, cost, "Texas Hold'em Raise");
                state.pot = state.pot.add(cost);
                state.playerStake = state.playerStake.add(cost);
            }
            state.playerStreetBet = state.playerStreetBet.add(cost);
            state.currentStreetBet = state.playerStreetBet;
            state.raisesThisStreet++;
            log.add(new TexasHoldemStateDto.ActionEntry("PLAYER", "RAISE", cost));
        } else { // CALL / CHECK
            if (playerOwed.compareTo(BigDecimal.ZERO) > 0) {
                characterService.deductCash(playerId, playerOwed, "Texas Hold'em Call");
                state.pot = state.pot.add(playerOwed);
                state.playerStake = state.playerStake.add(playerOwed);
            }
            state.playerStreetBet = state.currentStreetBet;
            log.add(new TexasHoldemStateDto.ActionEntry("PLAYER",
                playerOwed.compareTo(BigDecimal.ZERO) > 0 ? "CALL" : "CHECK", playerOwed));
        }

        // If player was responding to a bot raise, skip bots and advance
        if (wasAwaiting) {
            return advanceStreetAndSave(playerId, session, state, log);
        }

        // ── Bot actions ────────────────────────────────────────────────
        for (int i = 0; i < 4; i++) {
            if (state.botFolded.get(i)) continue;
            BigDecimal botOwed = state.currentStreetBet.subtract(state.botStreetBets.get(i)).max(BigDecimal.ZERO);
            double strength = estimateBotStrength(state.botCards.get(i), state.communityCards);
            String diff = state.botDifficulties.get(i);
            boolean isStubborn = state.botStubborn != null && state.botStubborn.get(i);
            String botAction = decideBotAction(diff, strength, botOwed, state.pot, state.initialBet, state.raisesThisStreet, isStubborn);

            switch (botAction) {
                case "FOLD":
                    state.botFolded.set(i, true);
                    log.add(new TexasHoldemStateDto.ActionEntry("BOT_" + i, "FOLD", BigDecimal.ZERO));
                    break;
                case "RAISE":
                    BigDecimal raiseAmt = botOwed.add(state.initialBet);
                    state.pot = state.pot.add(raiseAmt);
                    state.botStreetBets.set(i, state.botStreetBets.get(i).add(raiseAmt));
                    state.currentStreetBet = state.botStreetBets.get(i);
                    state.raisesThisStreet++;
                    log.add(new TexasHoldemStateDto.ActionEntry("BOT_" + i, "RAISE", raiseAmt));
                    break;
                case "CALL":
                    if (botOwed.compareTo(BigDecimal.ZERO) > 0) {
                        state.pot = state.pot.add(botOwed);
                        state.botStreetBets.set(i, state.currentStreetBet);
                    }
                    log.add(new TexasHoldemStateDto.ActionEntry("BOT_" + i, "CALL", botOwed));
                    break;
                default: // CHECK
                    log.add(new TexasHoldemStateDto.ActionEntry("BOT_" + i, "CHECK", BigDecimal.ZERO));
            }
        }

        // All bots folded → uncontested win
        if (state.botFolded.stream().allMatch(f -> f)) {
            BigDecimal payout = state.pot.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
            characterService.addCash(playerId, payout);
            session.setStatus("WON");
            session.setPayoutAmount(payout);
            session.setGameState(serializeTHState(state));
            gamblingRepository.save(session);
            return buildTHDtoFinal(session, state, "WON", payout, log);
        }

        // Check if player must respond to a bot raise
        BigDecimal nowOwed = state.currentStreetBet.subtract(state.playerStreetBet).max(BigDecimal.ZERO);
        if (nowOwed.compareTo(BigDecimal.ZERO) > 0) {
            state.awaitingPlayerResponse = true;
            session.setGameState(serializeTHState(state));
            gamblingRepository.save(session);
            return buildTHDto(session, state, log);
        }

        return advanceStreetAndSave(playerId, session, state, log);
    }

    private TexasHoldemStateDto advanceStreetAndSave(Long playerId, GamblingSession session,
            TexasHoldemState state, List<TexasHoldemStateDto.ActionEntry> log) {
        // Reset per-street betting
        state.currentStreetBet = BigDecimal.ZERO;
        state.playerStreetBet = BigDecimal.ZERO;
        for (int i = 0; i < 4; i++) state.botStreetBets.set(i, BigDecimal.ZERO);
        state.raisesThisStreet = 0;

        String next = advanceTHStreet(state);
        if ("SHOWDOWN".equals(next)) return resolveShowdown(playerId, session, state, log);

        state.street = next;
        session.setGameState(serializeTHState(state));
        gamblingRepository.save(session);
        return buildTHDto(session, state, log);
    }

    private TexasHoldemStateDto resolveShowdown(Long playerId, GamblingSession session,
            TexasHoldemState state, List<TexasHoldemStateDto.ActionEntry> log) {
        List<String> community = state.communityCards;
        List<String> playerAll = new ArrayList<>(state.playerCards);
        playerAll.addAll(community);
        int playerRank = bestHandRankFrom(playerAll);
        String playerHandName = pokerHandName(playerRank);

        int maxBotRank = -1;
        for (int i = 0; i < 4; i++) {
            if (!state.botFolded.get(i)) {
                List<String> ba = new ArrayList<>(state.botCards.get(i));
                ba.addAll(community);
                maxBotRank = Math.max(maxBotRank, bestHandRankFrom(ba));
            }
        }

        BigDecimal payout;
        String result;
        if (playerRank > maxBotRank) {
            payout = state.pot.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
            result = "WON";
        } else if (playerRank == maxBotRank) {
            long tied = 0;
            for (int i = 0; i < 4; i++) {
                if (!state.botFolded.get(i)) {
                    List<String> ba = new ArrayList<>(state.botCards.get(i)); ba.addAll(community);
                    if (bestHandRankFrom(ba) == playerRank) tied++;
                }
            }
            payout = state.pot.multiply(new BigDecimal("0.95"))
                .divide(BigDecimal.valueOf(1 + tied), 2, RoundingMode.HALF_UP);
            result = "PUSH";
        } else {
            payout = BigDecimal.ZERO;
            result = "LOST";
        }

        if (payout.compareTo(BigDecimal.ZERO) > 0) characterService.addCash(playerId, payout);
        session.setStatus(result);
        session.setPayoutAmount(payout);
        session.setGameState(serializeTHState(state));
        gamblingRepository.save(session);

        // Build showdown DTO with revealed bot cards
        List<TexasHoldemStateDto.BotInfo> bots = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String personality = state.botPersonalities != null ? state.botPersonalities.get(i) : "Bot " + (i+1);
            if (state.botFolded.get(i)) {
                bots.add(new TexasHoldemStateDto.BotInfo(i, true, List.of("??", "??"), null, false, personality));
            } else {
                List<String> ba = new ArrayList<>(state.botCards.get(i)); ba.addAll(community);
                int rank = bestHandRankFrom(ba);
                bots.add(new TexasHoldemStateDto.BotInfo(i, false, new ArrayList<>(state.botCards.get(i)), pokerHandName(rank), rank > playerRank, personality));
            }
        }
        BigDecimal netChange = payout.subtract(state.playerStake);
        return new TexasHoldemStateDto(session.getId(), state.playerCards, community,
            state.pot, state.playerStake, state.initialBet,
            BigDecimal.ZERO, state.initialBet,
            bots, "SHOWDOWN", result, false, log,
            playerHandName, List.of(), payout, netChange);
    }

    // ── Bot AI ─────────────────────────────────────────────────────────
    private String decideBotAction(String difficulty, double strength, BigDecimal toCall,
            BigDecimal pot, BigDecimal initialBet, int raisesThisStreet, boolean stubborn) {
        Random rng = ThreadLocalRandom.current();
        boolean hasToCall = toCall.compareTo(BigDecimal.ZERO) > 0;
        boolean canRaise  = raisesThisStreet < 4;

        // Stubborn bot: always calls/checks, never folds — goes to the bitter end
        if (stubborn) {
            if (canRaise && strength >= 0.70) return "RAISE";
            return hasToCall ? "CALL" : "CHECK";
        }

        double foldTh, callTh, raiseTh, bluff;
        switch (difficulty) {
            case "EASY" -> { foldTh = 0.28; callTh = 0.42; raiseTh = 0.76; bluff = 0.12; }
            case "HARD" -> { foldTh = 0.18; callTh = 0.30; raiseTh = 0.54; bluff = 0.07; }
            default      -> { foldTh = 0.28; callTh = 0.38; raiseTh = 0.65; bluff = 0.05; }
        }
        // HARD bots use pot odds to loosen calling range
        if ("HARD".equals(difficulty) && hasToCall) {
            double potOdds = toCall.doubleValue() / (pot.doubleValue() + toCall.doubleValue());
            if (potOdds < 0.22) foldTh -= 0.06;
        }
        if (canRaise && rng.nextDouble() < bluff) return "RAISE";
        if (canRaise && strength >= raiseTh)      return "RAISE";
        if (strength >= callTh)                    return "CALL";
        if (hasToCall && strength < foldTh)        return "FOLD";
        return hasToCall ? "CALL" : "CHECK";
    }

    // ── Street advance ─────────────────────────────────────────────────
    private String advanceTHStreet(TexasHoldemState state) {
        return switch (state.street) {
            case "PREFLOP" -> {
                state.communityCards.add(state.deck.remove(0));
                state.communityCards.add(state.deck.remove(0));
                state.communityCards.add(state.deck.remove(0));
                yield "FLOP";
            }
            case "FLOP" -> { state.communityCards.add(state.deck.remove(0)); yield "TURN"; }
            case "TURN" -> { state.communityCards.add(state.deck.remove(0)); yield "RIVER"; }
            default      -> "SHOWDOWN";
        };
    }

    // ── Hand strength estimator for bots ───────────────────────────────
    private double estimateBotStrength(List<String> hole, List<String> community) {
        if (community.isEmpty()) {
            int r1 = cardRank(hole.get(0).charAt(0)), r2 = cardRank(hole.get(1).charAt(0));
            double base = (r1 + r2 - 4.0) / 24.0;
            if (r1 == r2) base += 0.30;
            if (hole.get(0).charAt(1) == hole.get(1).charAt(1)) base += 0.08;
            return Math.min(1.0, base);
        }
        List<String> all = new ArrayList<>(hole); all.addAll(community);
        return bestHandRankFrom(all) / 9.0;
    }

    // ── Best 5-card hand from N cards ──────────────────────────────────
    private int bestHandRankFrom(List<String> cards) {
        if (cards.size() == 5) return evaluatePokerHand(cards);
        int best = 0, n = cards.size();
        for (int i = 0; i < n-4; i++)
          for (int j = i+1; j < n-3; j++)
            for (int k = j+1; k < n-2; k++)
              for (int l = k+1; l < n-1; l++)
                for (int m = l+1; m < n; m++)
                    best = Math.max(best, evaluatePokerHand(List.of(cards.get(i),cards.get(j),cards.get(k),cards.get(l),cards.get(m))));
        return best;
    }

    // ── Player hand description ─────────────────────────────────────────
    private String describeCurrentHand(List<String> playerCards, List<String> community) {
        if (community.isEmpty()) {
            int r1 = cardRank(playerCards.get(0).charAt(0)), r2 = cardRank(playerCards.get(1).charAt(0));
            if (r1 == r2) return "Taschenpaar (" + rankLabel(r1) + "s)";
            return "Highcard (" + rankLabel(Math.max(r1, r2)) + " hoch)";
        }
        List<String> all = new ArrayList<>(playerCards); all.addAll(community);
        return pokerHandName(bestHandRankFrom(all));
    }

    private String rankLabel(int r) {
        return switch (r) {
            case 14 -> "Ass"; case 13 -> "König"; case 12 -> "Dame"; case 11 -> "Bube";
            case 10 -> "Zehn"; case 9 -> "Neun"; case 8 -> "Acht"; case 7 -> "Sieben";
            case 6  -> "Sechs"; case 5 -> "Fünf"; case 4 -> "Vier"; case 3 -> "Drei"; case 2 -> "Zwei";
            default -> String.valueOf(r);
        };
    }

    // ── Draw detector ──────────────────────────────────────────────────
    private List<TexasHoldemStateDto.DrawInfo> detectDraws(List<String> playerCards, List<String> community) {
        List<TexasHoldemStateDto.DrawInfo> draws = new ArrayList<>();
        if (community.isEmpty() || community.size() >= 5) return draws;

        List<String> available = new ArrayList<>(playerCards); available.addAll(community);
        int remaining = 52 - available.size();
        if (remaining <= 0) return draws;

        int currentRank = bestHandRankFrom(available);

        // Flush draw
        if (currentRank < 5) {
            Map<Character, Long> suits = available.stream()
                .collect(Collectors.groupingBy(c -> c.charAt(1), Collectors.counting()));
            for (Map.Entry<Character, Long> e : suits.entrySet()) {
                if (e.getValue() == 4) {
                    int outs = 9;
                    draws.add(new TexasHoldemStateDto.DrawInfo("FLUSH_DRAW", outs,
                        Math.round(100.0 * outs / remaining * 10) / 10.0, "Flush Draw"));
                    break;
                }
            }
        }

        // Straight draws
        if (currentRank < 4) {
            Set<Integer> rankSet = new HashSet<>();
            for (String c : available) rankSet.add(cardRank(c.charAt(0)));
            if (rankSet.contains(14)) rankSet.add(1);

            boolean foundOpen = false, foundGut = false;
            for (int low = 1; low <= 10 && !foundOpen; low++) {
                int have = 0, missing = -1;
                for (int r = low; r <= low + 4; r++) {
                    if (rankSet.contains(r)) have++; else missing = r;
                }
                if (have == 4 && missing >= 0) {
                    boolean open = (missing == low || missing == low + 4);
                    if (open) {
                        draws.add(new TexasHoldemStateDto.DrawInfo("OPEN_STRAIGHT", 8,
                            Math.round(100.0 * 8 / remaining * 10) / 10.0, "Beidseitiger Straight Draw"));
                        foundOpen = true;
                    } else if (!foundGut) {
                        draws.add(new TexasHoldemStateDto.DrawInfo("GUTSHOT", 4,
                            Math.round(100.0 * 4 / remaining * 10) / 10.0, "Innerer Straight Draw"));
                        foundGut = true;
                    }
                }
            }
        }

        // Overcards (draw to top pair when holding nothing)
        if (currentRank == 0 && community.size() >= 3) {
            int maxComm = community.stream().mapToInt(c -> cardRank(c.charAt(0))).max().orElse(0);
            if (playerCards.stream().allMatch(c -> cardRank(c.charAt(0)) > maxComm)) {
                draws.add(new TexasHoldemStateDto.DrawInfo("OVERCARDS", 6,
                    Math.round(100.0 * 6 / remaining * 10) / 10.0, "Zwei Overcards → Top Pair"));
            }
        }

        return draws;
    }

    // ── DTO builders ───────────────────────────────────────────────────
    private TexasHoldemStateDto buildTHDto(GamblingSession session, TexasHoldemState state,
            List<TexasHoldemStateDto.ActionEntry> log) {
        List<TexasHoldemStateDto.BotInfo> bots = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String pers = state.botPersonalities != null ? state.botPersonalities.get(i) : "Bot " + (i+1);
            bots.add(new TexasHoldemStateDto.BotInfo(i, state.botFolded.get(i), List.of("??", "??"), null, false, pers));
        }
        BigDecimal toCall = state.currentStreetBet.subtract(state.playerStreetBet).max(BigDecimal.ZERO);
        BigDecimal raiseCost = toCall.add(state.initialBet);
        return new TexasHoldemStateDto(
            session.getId(), state.playerCards, state.communityCards,
            state.pot, state.playerStake, state.initialBet,
            toCall, raiseCost, bots, state.street, "IN_PROGRESS",
            state.awaitingPlayerResponse, log,
            describeCurrentHand(state.playerCards, state.communityCards),
            detectDraws(state.playerCards, state.communityCards),
            null, null);
    }

    private TexasHoldemStateDto buildTHDtoFinal(GamblingSession session, TexasHoldemState state,
            String result, BigDecimal payout, List<TexasHoldemStateDto.ActionEntry> log) {
        List<TexasHoldemStateDto.BotInfo> bots = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String pers = state.botPersonalities != null ? state.botPersonalities.get(i) : "Bot " + (i+1);
            bots.add(new TexasHoldemStateDto.BotInfo(i, state.botFolded.get(i), List.of("??", "??"), null, false, pers));
        }
        BigDecimal netChange = payout.subtract(state.playerStake);
        return new TexasHoldemStateDto(
            session.getId(), state.playerCards, state.communityCards,
            state.pot, state.playerStake, state.initialBet,
            BigDecimal.ZERO, state.initialBet, bots, state.street, result,
            false, log, null, List.of(), payout, netChange);
    }

    // ── Backward compat: init missing fields on old sessions ───────────
    private void initMissingTHFields(TexasHoldemState s) {
        if (s.currentStreetBet == null) s.currentStreetBet = BigDecimal.ZERO;
        if (s.playerStreetBet  == null) s.playerStreetBet  = BigDecimal.ZERO;
        if (s.botStreetBets    == null) s.botStreetBets     = new ArrayList<>(List.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        if (s.botDifficulties  == null) s.botDifficulties   = new ArrayList<>(List.of("MEDIUM","MEDIUM","MEDIUM","MEDIUM"));
        if (s.botPersonalities == null) s.botPersonalities  = new ArrayList<>(List.of("Bot 1","Bot 2","Bot 3","Bot 4"));
        if (s.botStubborn      == null) {
            List<Boolean> sb = new ArrayList<>(List.of(false, false, false, false));
            sb.set(ThreadLocalRandom.current().nextInt(4), true);
            s.botStubborn = sb;
        }
    }

    private String serializeTHState(TexasHoldemState state) {
        try { return objectMapper.writeValueAsString(state); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
    private TexasHoldemState deserializeTHState(String json) {
        try { return objectMapper.readValue(json, TexasHoldemState.class); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    // ======================== INNER STATE CLASSES ========================

    public static class BlackjackState {
        public List<String> deck;
        public List<String> playerHand;
        public List<String> dealerHand;
        public BlackjackState() {}
        public BlackjackState(List<String> deck, List<String> playerHand, List<String> dealerHand) {
            this.deck = deck; this.playerHand = playerHand; this.dealerHand = dealerHand;
        }
    }

    public static class TexasHoldemState {
        public List<String> deck;
        public List<String> playerCards;
        public List<List<String>> botCards;
        public List<String> communityCards;
        public BigDecimal pot;
        public BigDecimal playerStake;
        public BigDecimal initialBet;
        public List<Boolean> botFolded;
        public String street;
        // Betting per street
        public BigDecimal currentStreetBet;
        public BigDecimal playerStreetBet;
        public List<BigDecimal> botStreetBets;
        public int raisesThisStreet;
        public boolean awaitingPlayerResponse;
        // Bot metadata
        public List<String> botDifficulties;
        public List<String> botPersonalities;
        public List<Boolean> botStubborn;
        public TexasHoldemState() {}
    }
}
