package com.financegame.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financegame.repository.GamblingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GamblingServiceTest {

    @Mock GamblingRepository gamblingRepository;
    @Mock CharacterService characterService;

    GamblingService service;

    @BeforeEach
    void setUp() {
        service = new GamblingService(gamblingRepository, characterService, new ObjectMapper());
    }

    // ── Bet validation ───────────────────────────────────────────────────────

    @Test
    void playSlots_nullBet_throws() {
        assertThatThrownBy(() -> service.playSlots(1L, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Mindesteinsatz");
    }

    @Test
    void playSlots_belowMinBet_throws() {
        assertThatThrownBy(() -> service.playSlots(1L, new BigDecimal("0.99")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Mindesteinsatz");
    }

    @Test
    void playSlots_aboveMaxBet_throws() {
        assertThatThrownBy(() -> service.playSlots(1L, new BigDecimal("10000.01")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Maximaleinsatz");
    }

    // ── Blackjack total calculation ──────────────────────────────────────────

    @Test
    void calcBjTotal_noAce_sumsCorrectly() {
        assertThat(service.calcBjTotal(List.of("KH", "7S"))).isEqualTo(17);
    }

    @Test
    void calcBjTotal_softAce_counts11() {
        // A + 6 = 17 (ace as 11)
        assertThat(service.calcBjTotal(List.of("AH", "6S"))).isEqualTo(17);
    }

    @Test
    void calcBjTotal_hardAce_counts1ToAvoidBust() {
        // A + K + 5 = 16 (ace forced to 1, else 26)
        assertThat(service.calcBjTotal(List.of("AH", "KS", "5D"))).isEqualTo(16);
    }

    @Test
    void calcBjTotal_twoAces_oneSoft() {
        // A + A = 12 (one 11, one 1)
        assertThat(service.calcBjTotal(List.of("AH", "AS"))).isEqualTo(12);
    }

    @Test
    void calcBjTotal_naturalBlackjack() {
        assertThat(service.calcBjTotal(List.of("AH", "KS"))).isEqualTo(21);
    }

    @Test
    void calcBjTotal_aceLowToPreventBust() {
        // A + 9 + 5 = 15 (ace must count as 1)
        assertThat(service.calcBjTotal(List.of("AH", "9S", "5D"))).isEqualTo(15);
    }

    // ── Poker hand evaluation ────────────────────────────────────────────────

    @Test
    void evaluatePokerHand_royalFlush() {
        assertThat(service.evaluatePokerHand(List.of("AH","KH","QH","JH","TH"))).isEqualTo(9);
    }

    @Test
    void evaluatePokerHand_straightFlush() {
        assertThat(service.evaluatePokerHand(List.of("9H","8H","7H","6H","5H"))).isEqualTo(8);
    }

    @Test
    void evaluatePokerHand_fourOfAKind() {
        assertThat(service.evaluatePokerHand(List.of("7H","7D","7C","7S","2H"))).isEqualTo(7);
    }

    @Test
    void evaluatePokerHand_fullHouse() {
        assertThat(service.evaluatePokerHand(List.of("KH","KD","KC","2H","2D"))).isEqualTo(6);
    }

    @Test
    void evaluatePokerHand_flush() {
        assertThat(service.evaluatePokerHand(List.of("2H","5H","7H","9H","JH"))).isEqualTo(5);
    }

    @Test
    void evaluatePokerHand_straight() {
        assertThat(service.evaluatePokerHand(List.of("5H","6D","7C","8S","9H"))).isEqualTo(4);
    }

    @Test
    void evaluatePokerHand_aceLowStraight() {
        // A-2-3-4-5 wheel straight
        assertThat(service.evaluatePokerHand(List.of("AH","2D","3C","4S","5H"))).isEqualTo(4);
    }

    @Test
    void evaluatePokerHand_threeOfAKind() {
        assertThat(service.evaluatePokerHand(List.of("8H","8D","8C","3S","KH"))).isEqualTo(3);
    }

    @Test
    void evaluatePokerHand_twoPair() {
        assertThat(service.evaluatePokerHand(List.of("JH","JD","4C","4S","AH"))).isEqualTo(2);
    }

    @Test
    void evaluatePokerHand_onePair() {
        assertThat(service.evaluatePokerHand(List.of("TH","TD","3C","7S","AH"))).isEqualTo(1);
    }

    @Test
    void evaluatePokerHand_highCard() {
        assertThat(service.evaluatePokerHand(List.of("2H","5D","7C","9S","JH"))).isEqualTo(0);
    }
}
