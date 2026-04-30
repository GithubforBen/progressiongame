package com.financegame.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financegame.dto.BlackjackStateDto;
import com.financegame.dto.PokerResultDto;
import com.financegame.dto.RouletteRequest;
import com.financegame.dto.RouletteResultDto;
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

    // ── Roulette constants ─────────────────────────────────────────────────
    private static final BigDecimal ROULETTE_MAX_TOTAL = new BigDecimal("10000.00");
    private static final Set<Integer> RED_NUMBERS = Set.of(
        1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36);
    private static final Set<Integer> BLACK_NUMBERS = Set.of(
        2,4,6,8,10,11,13,15,17,20,22,24,26,28,29,31,33,35);

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

        List<Integer> playerKey = evaluatePokerHandKey(playerCards);
        List<Integer> aiKey     = evaluatePokerHandKey(aiCards);
        int cmp = compareHandKeys(playerKey, aiKey);

        BigDecimal payout;
        String result;
        if (cmp > 0) {
            payout = bet.multiply(new BigDecimal("1.95")).setScale(2, RoundingMode.HALF_UP);
            result = "WON";
        } else if (cmp == 0) {
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
            pokerHandName(playerKey.get(0)),
            pokerHandName(aiKey.get(0)),
            result,
            bet,
            payout,
            payout.subtract(bet)
        );
    }

    // ======================== ROULETTE ========================

    @Transactional
    public RouletteResultDto playRoulette(Long playerId, RouletteRequest request) {
        List<RouletteRequest.RouletteBet> bets = request.bets();

        BigDecimal totalBet = BigDecimal.ZERO;
        for (RouletteRequest.RouletteBet b : bets) {
            validateRouletteBet(b);
            totalBet = totalBet.add(b.amount());
        }
        if (totalBet.compareTo(ROULETTE_MAX_TOTAL) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Gesamteinsatz darf 10.000 € nicht überschreiten");
        }

        characterService.deductCash(playerId, totalBet, "Roulette");

        int winning = ThreadLocalRandom.current().nextInt(37);

        BigDecimal totalPayout = BigDecimal.ZERO;
        List<RouletteResultDto.BetResult> results = new ArrayList<>();
        for (RouletteRequest.RouletteBet b : bets) {
            BigDecimal payout = resolveRouletteBet(b, winning);
            totalPayout = totalPayout.add(payout);
            results.add(new RouletteResultDto.BetResult(
                b.type(), b.numbers(), b.amount(),
                payout.compareTo(BigDecimal.ZERO) > 0, payout));
        }

        if (totalPayout.compareTo(BigDecimal.ZERO) > 0) {
            characterService.addCash(playerId, totalPayout);
        }

        String color = winning == 0 ? "green"
            : RED_NUMBERS.contains(winning) ? "red" : "black";
        BigDecimal netChange = totalPayout.subtract(totalBet);

        GamblingSession session = new GamblingSession();
        session.setPlayerId(playerId);
        session.setGameType("ROULETTE");
        session.setStatus(netChange.compareTo(BigDecimal.ZERO) >= 0 ? "WON" : "LOST");
        session.setBetAmount(totalBet);
        session.setPayoutAmount(totalPayout);
        gamblingRepository.save(session);

        return new RouletteResultDto(winning, color, totalBet, totalPayout, netChange, results);
    }

    private BigDecimal resolveRouletteBet(RouletteRequest.RouletteBet bet, int w) {
        boolean hits = switch (bet.type()) {
            case "STRAIGHT", "SPLIT", "STREET", "CORNER", "SIX_LINE", "TRIO"
                -> bet.numbers().contains(w);
            case "RED"      -> RED_NUMBERS.contains(w);
            case "BLACK"    -> BLACK_NUMBERS.contains(w);
            case "EVEN"     -> w != 0 && w % 2 == 0;
            case "ODD"      -> w % 2 == 1;
            case "LOW"      -> w >= 1 && w <= 18;
            case "HIGH"     -> w >= 19 && w <= 36;
            case "DOZEN_1"  -> w >= 1 && w <= 12;
            case "DOZEN_2"  -> w >= 13 && w <= 24;
            case "DOZEN_3"  -> w >= 25 && w <= 36;
            case "COLUMN_1" -> w != 0 && w % 3 == 1;
            case "COLUMN_2" -> w != 0 && w % 3 == 2;
            case "COLUMN_3" -> w != 0 && w % 3 == 0;
            default         -> false;
        };
        if (!hits) return BigDecimal.ZERO;

        int multiplier = switch (bet.type()) {
            case "STRAIGHT"                          -> 36;
            case "SPLIT"                             -> 18;
            case "STREET", "TRIO"                   -> 12;
            case "CORNER"                            -> 9;
            case "SIX_LINE"                          -> 6;
            case "DOZEN_1", "DOZEN_2", "DOZEN_3",
                 "COLUMN_1", "COLUMN_2", "COLUMN_3" -> 3;
            default                                  -> 2;
        };
        return bet.amount().multiply(BigDecimal.valueOf(multiplier))
                           .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateRouletteBet(RouletteRequest.RouletteBet bet) {
        List<Integer> nums = bet.numbers() != null ? bet.numbers() : Collections.emptyList();
        switch (bet.type()) {
            case "STRAIGHT" -> {
                if (nums.size() != 1) throw rouletteBad("STRAIGHT braucht genau 1 Zahl");
                if (nums.get(0) < 0 || nums.get(0) > 36) throw rouletteBad("Ungültige Zahl");
            }
            case "SPLIT" -> {
                if (nums.size() != 2) throw rouletteBad("SPLIT braucht genau 2 Zahlen");
                validateSplitAdjacency(nums.get(0), nums.get(1));
            }
            case "STREET" -> {
                if (nums.size() != 3) throw rouletteBad("STREET braucht genau 3 Zahlen");
                validateStreetNums(nums);
            }
            case "CORNER" -> {
                if (nums.size() != 4) throw rouletteBad("CORNER braucht genau 4 Zahlen");
                validateCornerNums(nums);
            }
            case "SIX_LINE" -> {
                if (nums.size() != 6) throw rouletteBad("SIX_LINE braucht genau 6 Zahlen");
                validateSixLineNums(nums);
            }
            case "TRIO" -> {
                if (nums.size() != 3) throw rouletteBad("TRIO braucht genau 3 Zahlen");
                List<Integer> sorted = nums.stream().sorted().toList();
                if (!sorted.equals(List.of(0, 1, 2)) && !sorted.equals(List.of(0, 2, 3)))
                    throw rouletteBad("TRIO: nur 0-1-2 oder 0-2-3 erlaubt");
            }
            case "RED","BLACK","EVEN","ODD","LOW","HIGH",
                 "DOZEN_1","DOZEN_2","DOZEN_3",
                 "COLUMN_1","COLUMN_2","COLUMN_3" -> { /* no number validation needed */ }
            default -> throw rouletteBad("Unbekannter Wetttyp: " + bet.type());
        }
    }

    private int rouletteCol(int n) { return (n - 1) % 3; }
    private int rouletteRow(int n) { return (n - 1) / 3; }

    private void validateSplitAdjacency(int a, int b) {
        if (a < 1 || a > 36 || b < 1 || b > 36) throw rouletteBad("SPLIT: Zahlen müssen 1–36 sein");
        boolean horizontal = rouletteRow(a) == rouletteRow(b) && Math.abs(rouletteCol(a) - rouletteCol(b)) == 1;
        boolean vertical   = rouletteCol(a) == rouletteCol(b) && Math.abs(rouletteRow(a) - rouletteRow(b)) == 1;
        if (!horizontal && !vertical)
            throw rouletteBad("SPLIT: " + a + " und " + b + " sind nicht benachbart");
    }

    private void validateStreetNums(List<Integer> nums) {
        List<Integer> sorted = nums.stream().sorted().toList();
        int first = sorted.get(0);
        if (first < 1 || (first - 1) % 3 != 0) throw rouletteBad("STREET: ungültige Startzahl");
        if (!sorted.equals(List.of(first, first + 1, first + 2)))
            throw rouletteBad("STREET: muss eine vollständige Reihe sein");
    }

    private void validateCornerNums(List<Integer> nums) {
        List<Integer> sorted = nums.stream().sorted().toList();
        int tl = sorted.get(0);
        if (tl < 1 || tl > 36) throw rouletteBad("CORNER: ungültige Zahl");
        if (rouletteCol(tl) == 2) throw rouletteBad("CORNER: nicht möglich ab Spalte 3");
        if (rouletteRow(tl) >= 11) throw rouletteBad("CORNER: keine weitere Reihe verfügbar");
        List<Integer> expected = List.of(tl, tl + 1, tl + 3, tl + 4);
        if (!sorted.equals(expected)) throw rouletteBad("CORNER: keine gültige 2×2-Ecke");
    }

    private void validateSixLineNums(List<Integer> nums) {
        List<Integer> sorted = nums.stream().sorted().toList();
        int first = sorted.get(0);
        if (first < 1 || (first - 1) % 3 != 0) throw rouletteBad("SIX_LINE: ungültige Startzahl");
        if (rouletteRow(first) >= 11) throw rouletteBad("SIX_LINE: keine weitere Reihe verfügbar");
        List<Integer> expected = List.of(first, first+1, first+2, first+3, first+4, first+5);
        if (!sorted.equals(expected)) throw rouletteBad("SIX_LINE: muss zwei aufeinanderfolgende Reihen sein");
    }

    private ResponseStatusException rouletteBad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
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

    /**
     * Returns a lexicographically comparable hand key: [handRank, tiebreaker1, tiebreaker2, ...].
     * handRank 0=High Card … 9=Royal Flush. Tiebreakers follow the standard poker rules
     * (primary value, secondary value, then kickers high-to-low).
     */
    List<Integer> evaluatePokerHandKey(List<String> cards) {
        int[] ranks = cards.stream().mapToInt(c -> cardRank(c.charAt(0))).sorted().toArray();
        Map<Character, Long> suitFreq = cards.stream()
            .collect(Collectors.groupingBy(c -> c.charAt(1), Collectors.counting()));
        Map<Integer, Long> rankFreq = IntStream.of(ranks).boxed()
            .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

        boolean isFlush = suitFreq.values().stream().anyMatch(v -> v == 5);
        boolean isAceLow = ranks[4] == 14 && ranks[3] == 5 && ranks[2] == 4 && ranks[1] == 3 && ranks[0] == 2;
        boolean isStraight = isAceLow || (ranks[4] - ranks[0] == 4 && rankFreq.size() == 5);
        int straightHigh = isAceLow ? 5 : ranks[4];

        long pairs   = rankFreq.values().stream().filter(v -> v == 2).count();
        boolean hasThree = rankFreq.values().stream().anyMatch(v -> v == 3);
        boolean hasFour  = rankFreq.values().stream().anyMatch(v -> v == 4);

        // Sort card ranks: frequency DESC, then rank value DESC (for tiebreaker order)
        List<Integer> byFreqDesc = rankFreq.entrySet().stream()
            .sorted((a, b) -> a.getValue().equals(b.getValue())
                ? b.getKey().compareTo(a.getKey())
                : b.getValue().compareTo(a.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (isFlush && isStraight) {
            // Royal Flush = A-high straight flush
            return straightHigh == 14 ? List.of(9, 14) : List.of(8, straightHigh);
        }
        if (hasFour)              return prependRank(7, byFreqDesc); // quad + kicker
        if (hasThree && pairs==1) return prependRank(6, byFreqDesc); // trips + pair
        if (isFlush) {
            // Compare all 5 cards high → low
            List<Integer> key = new ArrayList<>();
            key.add(5);
            for (int i = ranks.length - 1; i >= 0; i--) key.add(ranks[i]);
            return key;
        }
        if (isStraight)           return List.of(4, straightHigh);
        if (hasThree)             return prependRank(3, byFreqDesc); // trips + 2 kickers
        if (pairs == 2)           return prependRank(2, byFreqDesc); // high pair, low pair, kicker
        if (pairs == 1)           return prependRank(1, byFreqDesc); // pair + 3 kickers
        // High card: all 5 ranks high → low
        List<Integer> key = new ArrayList<>();
        key.add(0);
        for (int i = ranks.length - 1; i >= 0; i--) key.add(ranks[i]);
        return key;
    }

    // Backward-compatible wrapper (returns hand rank 0–9 only)
    int evaluatePokerHand(List<String> cards) {
        return evaluatePokerHandKey(cards).get(0);
    }

    private List<Integer> prependRank(int handRank, List<Integer> tiebreakers) {
        List<Integer> key = new ArrayList<>();
        key.add(handRank);
        key.addAll(tiebreakers);
        return key;
    }

    /** Lexicographic comparison: positive if a > b, 0 if equal, negative if a < b. */
    private int compareHandKeys(List<Integer> a, List<Integer> b) {
        int len = Math.min(a.size(), b.size());
        for (int i = 0; i < len; i++) {
            int c = Integer.compare(a.get(i), b.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
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

        // Pick 4 personalities randomly: guarantee at least 1 LOW-risk and 1 HIGH-risk
        Random rng = ThreadLocalRandom.current();
        List<BotPersonality> lowPool  = PERSONALITIES.stream().filter(p -> "LOW".equals(p.riskProfile())).toList();
        List<BotPersonality> highPool = PERSONALITIES.stream().filter(p -> "HIGH".equals(p.riskProfile())).toList();
        List<BotPersonality> chosen = new ArrayList<>();
        chosen.add(lowPool.get(rng.nextInt(lowPool.size())));
        chosen.add(highPool.get(rng.nextInt(highPool.size())));
        while (chosen.size() < 4) chosen.add(PERSONALITIES.get(rng.nextInt(PERSONALITIES.size())));
        Collections.shuffle(chosen, rng);

        List<String> personalityKeys = chosen.stream().map(BotPersonality::name).collect(Collectors.toList());
        List<String> riskProfiles    = chosen.stream().map(BotPersonality::riskProfile).collect(Collectors.toList());
        List<String> personalities   = new ArrayList<>(personalityKeys);

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
        state.botPersonalityKeys = personalityKeys;
        state.botRiskProfiles = riskProfiles;
        state.botPersonalities = personalities;
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
            String pKey = (state.botPersonalityKeys != null) ? state.botPersonalityKeys.get(i) : "Kalkulierer";
            BotPersonality personality = findPersonality(pKey);
            double equity = estimateBotStrength(state.botCards.get(i), state.communityCards, state.deck);
            String botAction = decideBotAction(personality, equity, botOwed, state.pot,
                state.initialBet, state.raisesThisStreet, state.street);

            switch (botAction) {
                case "FOLD":
                    state.botFolded.set(i, true);
                    log.add(new TexasHoldemStateDto.ActionEntry("BOT_" + i, "FOLD", BigDecimal.ZERO));
                    break;
                case "RAISE":
                    boolean isBluff = equity < 0.35;
                    BigDecimal raiseSize = calculateBotRaiseAmount(personality, equity, state.pot, state.initialBet, isBluff);
                    BigDecimal raiseCost = botOwed.add(raiseSize);
                    state.pot = state.pot.add(raiseCost);
                    state.botStreetBets.set(i, state.botStreetBets.get(i).add(raiseCost));
                    state.currentStreetBet = state.botStreetBets.get(i);
                    state.raisesThisStreet++;
                    log.add(new TexasHoldemStateDto.ActionEntry("BOT_" + i, "RAISE", raiseCost));
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
        List<Integer> playerKey  = bestHandKeyFrom(playerAll);
        String playerHandName = pokerHandName(playerKey.get(0));

        // Find the best bot key across all active bots
        List<Integer> bestBotKey = List.of(-1);
        for (int i = 0; i < 4; i++) {
            if (!state.botFolded.get(i)) {
                List<String> ba = new ArrayList<>(state.botCards.get(i));
                ba.addAll(community);
                List<Integer> botKey = bestHandKeyFrom(ba);
                if (compareHandKeys(botKey, bestBotKey) > 0) bestBotKey = botKey;
            }
        }

        int cmp = compareHandKeys(playerKey, bestBotKey);
        BigDecimal payout;
        String result;
        if (cmp > 0) {
            payout = state.pot.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
            result = "WON";
        } else if (cmp == 0) {
            // Count bots that tie exactly with the player's hand key
            long tied = 0;
            for (int i = 0; i < 4; i++) {
                if (!state.botFolded.get(i)) {
                    List<String> ba = new ArrayList<>(state.botCards.get(i)); ba.addAll(community);
                    if (compareHandKeys(bestHandKeyFrom(ba), playerKey) == 0) tied++;
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
            String risk        = state.botRiskProfiles  != null ? state.botRiskProfiles.get(i)  : "MEDIUM";
            if (state.botFolded.get(i)) {
                bots.add(new TexasHoldemStateDto.BotInfo(i, true, List.of("??", "??"), null, false, personality, risk));
            } else {
                List<String> ba = new ArrayList<>(state.botCards.get(i)); ba.addAll(community);
                List<Integer> botKey = bestHandKeyFrom(ba);
                bots.add(new TexasHoldemStateDto.BotInfo(i, false, new ArrayList<>(state.botCards.get(i)),
                    pokerHandName(botKey.get(0)), compareHandKeys(botKey, playerKey) > 0, personality, risk));
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
    private String decideBotAction(BotPersonality p, double equity, BigDecimal toCall,
            BigDecimal pot, BigDecimal initialBet, int raisesThisStreet, String street) {
        Random rng = ThreadLocalRandom.current();
        boolean hasToCall = toCall.compareTo(BigDecimal.ZERO) > 0;
        boolean canRaise  = raisesThisStreet < 4;

        if ("PREFLOP".equals(street)) {
            // VPIP/PFR-based preflop decision
            double foldTh  = 1.0 - p.vpip();
            double raiseTh = foldTh + p.vpip() * p.pfrRatio();
            if (!hasToCall && equity < foldTh) return "CHECK";
            if (hasToCall  && equity < foldTh) return "FOLD";
            if (canRaise   && equity > raiseTh) return "RAISE";
            return hasToCall ? "CALL" : "CHECK";
        }

        // Postflop: equity vs pot odds
        double potOdds = hasToCall
            ? toCall.doubleValue() / (pot.doubleValue() + toCall.doubleValue())
            : 0.0;

        // Bluff with very weak hand (missed draw texture)
        if (canRaise && equity < 0.35 && rng.nextDouble() < p.bluffFrequency()) return "RAISE";

        // Facing a bet: fold if not getting correct price, adjusted by personality
        if (hasToCall) {
            double foldThreshold = potOdds + p.foldToBet() * 0.15;
            if (equity < foldThreshold) return "FOLD";
        }

        // Value raise when equity is strong enough (aggressive personalities raise more often)
        double raiseTh = Math.max(0.52, 0.82 - p.aggressionFactor() * 0.07);
        if (canRaise && equity > raiseTh) return "RAISE";

        // EV check: call if profitable, else fold/check
        double ev = equity * pot.doubleValue() - (1.0 - equity) * toCall.doubleValue();
        if (ev >= 0.0 || !hasToCall) return hasToCall ? "CALL" : "CHECK";
        return "FOLD";
    }

    private BigDecimal calculateBotRaiseAmount(BotPersonality p, double equity,
            BigDecimal pot, BigDecimal initialBet, boolean isBluff) {
        Random rng = ThreadLocalRandom.current();
        double fraction;
        if (isBluff)            fraction = 0.60 + rng.nextDouble() * 0.10;
        else if (equity > 0.80) fraction = 0.80 + rng.nextDouble() * 0.20;
        else if (equity > 0.65) fraction = 0.55 + rng.nextDouble() * 0.25;
        else                    fraction = 0.38 + rng.nextDouble() * 0.22;
        // Aggressive personalities bet bigger
        fraction = Math.min(1.8, fraction * (0.75 + p.aggressionFactor() * 0.09));
        BigDecimal amount = pot.multiply(BigDecimal.valueOf(fraction))
            .setScale(2, RoundingMode.HALF_UP);
        return amount.max(initialBet);
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

    // ── Equity estimators for bots ─────────────────────────────────────
    private double estimateBotStrength(List<String> hole, List<String> community, List<String> deck) {
        if (community.isEmpty()) return estimatePreflopEquity(hole);
        return estimatePostflopEquity(hole, community, deck);
    }

    private double estimatePreflopEquity(List<String> hole) {
        int r1 = cardRank(hole.get(0).charAt(0)), r2 = cardRank(hole.get(1).charAt(0));
        boolean suited    = hole.get(0).charAt(1) == hole.get(1).charAt(1);
        boolean paired    = r1 == r2;
        boolean connected = !paired && Math.abs(r1 - r2) <= 2;
        int hi = Math.max(r1, r2), lo = Math.min(r1, r2);
        double base = 0.40 * (hi - 2.0) / 12.0 + 0.20 * (lo - 2.0) / 12.0;
        if (paired)    base += 0.22;
        if (suited)    base += 0.05;
        if (connected) base += 0.04;
        return Math.min(1.0, Math.max(0.0, base));
    }

    private double estimatePostflopEquity(List<String> hole, List<String> community, List<String> deck) {
        // Monte Carlo: 80 random completions of the board vs one random opponent
        int wins = 0;
        final int TRIALS = 80;
        List<String> available = new ArrayList<>(deck);
        Random rng = ThreadLocalRandom.current();
        Collections.shuffle(available, rng);
        int pool = available.size();
        for (int t = 0; t < TRIALS; t++) {
            // Re-shuffle every 20 trials for variety
            if (t > 0 && t % 20 == 0) Collections.shuffle(available, rng);
            int idx = 0;
            List<String> simBoard = new ArrayList<>(community);
            while (simBoard.size() < 5 && idx < pool) simBoard.add(available.get(idx++));
            if (idx + 1 >= pool) break; // not enough cards to give villain 2
            List<String> villainHole = List.of(available.get(idx), available.get(idx + 1));
            List<String> myAll  = new ArrayList<>(hole);    myAll.addAll(simBoard);
            List<String> vilAll = new ArrayList<>(villainHole); vilAll.addAll(simBoard);
            int cmp = compareHandKeys(bestHandKeyFrom(myAll), bestHandKeyFrom(vilAll));
            if (cmp > 0)      wins += 2;
            else if (cmp == 0) wins += 1; // tie counts as half win
        }
        return wins / (2.0 * TRIALS);
    }

    // ── Best 5-card hand from N cards (full tiebreaker key) ────────────
    private List<Integer> bestHandKeyFrom(List<String> cards) {
        if (cards.size() == 5) return evaluatePokerHandKey(cards);
        List<Integer> best = List.of(-1);
        int n = cards.size();
        for (int i = 0; i < n-4; i++)
          for (int j = i+1; j < n-3; j++)
            for (int k = j+1; k < n-2; k++)
              for (int l = k+1; l < n-1; l++)
                for (int m = l+1; m < n; m++) {
                    List<Integer> key = evaluatePokerHandKey(
                        List.of(cards.get(i), cards.get(j), cards.get(k), cards.get(l), cards.get(m)));
                    if (compareHandKeys(key, best) > 0) best = key;
                }
        return best;
    }

    // Backward-compatible wrapper (returns best hand rank 0–9 only)
    private int bestHandRankFrom(List<String> cards) {
        return bestHandKeyFrom(cards).get(0);
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
            String risk = state.botRiskProfiles  != null ? state.botRiskProfiles.get(i)  : "MEDIUM";
            bots.add(new TexasHoldemStateDto.BotInfo(i, state.botFolded.get(i), List.of("??", "??"), null, false, pers, risk));
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
            String risk = state.botRiskProfiles  != null ? state.botRiskProfiles.get(i)  : "MEDIUM";
            bots.add(new TexasHoldemStateDto.BotInfo(i, state.botFolded.get(i), List.of("??", "??"), null, false, pers, risk));
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
        if (s.currentStreetBet  == null) s.currentStreetBet  = BigDecimal.ZERO;
        if (s.playerStreetBet   == null) s.playerStreetBet   = BigDecimal.ZERO;
        if (s.botStreetBets     == null) s.botStreetBets      = new ArrayList<>(List.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        if (s.botPersonalities  == null) s.botPersonalities   = new ArrayList<>(List.of("Bot 1","Bot 2","Bot 3","Bot 4"));
        if (s.botPersonalityKeys == null) s.botPersonalityKeys = new ArrayList<>(List.of("Kalkulierer","Kalkulierer","Kalkulierer","Kalkulierer"));
        if (s.botRiskProfiles   == null) s.botRiskProfiles    = new ArrayList<>(List.of("MEDIUM","MEDIUM","MEDIUM","MEDIUM"));
    }

    private String serializeTHState(TexasHoldemState state) {
        try { return objectMapper.writeValueAsString(state); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
    private TexasHoldemState deserializeTHState(String json) {
        try { return objectMapper.readValue(json, TexasHoldemState.class); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    // ======================== BOT PERSONALITIES ========================

    private record BotPersonality(
        String name,
        double vpip,             // preflop play frequency (0.0-1.0)
        double pfrRatio,         // raise% / vpip% (aggressiveness preflop)
        double aggressionFactor, // postflop raise/call ratio (0.5-4.5)
        double bluffFrequency,   // bluff tendency with weak hand (0.0-0.5)
        double foldToBet,        // fold-to-bet tendency (0.0-0.9)
        String riskProfile       // "LOW" / "MEDIUM" / "HIGH" for display
    ) {}

    private static final List<BotPersonality> PERSONALITIES = List.of(
        new BotPersonality("Bedächtiger",    0.14, 0.86, 1.5, 0.05, 0.72, "LOW"),
        new BotPersonality("Stratege",       0.22, 0.82, 2.6, 0.16, 0.56, "MEDIUM"),
        new BotPersonality("Kalkulierer",    0.26, 0.78, 2.9, 0.22, 0.50, "MEDIUM"),
        new BotPersonality("Eiskalter",      0.28, 0.75, 3.1, 0.27, 0.46, "MEDIUM"),
        new BotPersonality("Haifisch",       0.38, 0.74, 3.3, 0.33, 0.37, "HIGH"),
        new BotPersonality("Draufgänger",    0.56, 0.72, 4.2, 0.50, 0.22, "HIGH"),
        new BotPersonality("Glücksspieler",  0.52, 0.18, 0.6, 0.07, 0.16, "MEDIUM"),
        new BotPersonality("Risikofreak",    0.60, 0.42, 2.0, 0.43, 0.27, "HIGH")
    );

    private BotPersonality findPersonality(String name) {
        return PERSONALITIES.stream()
            .filter(p -> p.name().equals(name))
            .findFirst()
            .orElse(PERSONALITIES.get(2)); // Kalkulierer as neutral fallback
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
        public List<String> botPersonalityKeys;  // personality name → lookup in PERSONALITIES
        public List<String> botRiskProfiles;     // LOW / MEDIUM / HIGH for frontend
        public List<String> botPersonalities;    // display names (same as keys currently)
        // Legacy fields kept nullable for backward-compat with old serialized sessions
        public List<String> botDifficulties;
        public List<Boolean> botStubborn;
        public TexasHoldemState() {}
    }
}
