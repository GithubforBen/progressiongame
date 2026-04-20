package com.financegame.domain.condition;

import com.financegame.domain.GameContext;
import com.financegame.entity.GameCharacter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionTest {

    // ── HasCertCondition ──────────────────────────────────────────────────────

    @Test
    void hasCert_presentInList_true() {
        GameContext ctx = ctx(List.of("WEITERBILDUNG_CRYPTO_1"), null, false);
        assertThat(new HasCertCondition("WEITERBILDUNG_CRYPTO_1").isMet(ctx)).isTrue();
    }

    @Test
    void hasCert_missingFromList_false() {
        GameContext ctx = ctx(List.of("WEITERBILDUNG_EXCEL_1"), null, false);
        assertThat(new HasCertCondition("WEITERBILDUNG_CRYPTO_1").isMet(ctx)).isFalse();
    }

    // ── MinSchufaCondition ────────────────────────────────────────────────────

    @Test
    void minSchufa_scoreMeetsThreshold_true() {
        GameContext ctx = ctx(List.of(), characterWithSchufa(500), false);
        assertThat(new MinSchufaCondition(300).isMet(ctx)).isTrue();
    }

    @Test
    void minSchufa_exactlyAtThreshold_true() {
        GameContext ctx = ctx(List.of(), characterWithSchufa(300), false);
        assertThat(new MinSchufaCondition(300).isMet(ctx)).isTrue();
    }

    @Test
    void minSchufa_scoreBelowThreshold_false() {
        GameContext ctx = ctx(List.of(), characterWithSchufa(299), false);
        assertThat(new MinSchufaCondition(300).isMet(ctx)).isFalse();
    }

    // ── NotInJailCondition ────────────────────────────────────────────────────

    @Test
    void notInJail_jailMonthsZero_true() {
        GameCharacter c = new GameCharacter();
        c.setJailMonthsRemaining(0);
        assertThat(new NotInJailCondition().isMet(ctx(List.of(), c, false))).isTrue();
    }

    @Test
    void notInJail_jailMonthsPositive_false() {
        GameCharacter c = new GameCharacter();
        c.setJailMonthsRemaining(3);
        assertThat(new NotInJailCondition().isMet(ctx(List.of(), c, false))).isFalse();
    }

    // ── NotTravelingCondition ─────────────────────────────────────────────────

    @Test
    void notTraveling_notTraveling_true() {
        assertThat(new NotTravelingCondition().isMet(ctx(List.of(), null, false))).isTrue();
    }

    @Test
    void notTraveling_traveling_false() {
        assertThat(new NotTravelingCondition().isMet(ctx(List.of(), null, true))).isFalse();
    }

    // ── EducationLevelCondition ───────────────────────────────────────────────

    @Test
    void educationLevel_exactMatch_true() {
        GameContext ctx = ctx(List.of("BACHELOR"), null, false);
        assertThat(new EducationLevelCondition("BACHELOR").isMet(ctx)).isTrue();
    }

    @Test
    void educationLevel_prefixMatch_true() {
        GameContext ctx = ctx(List.of("BACHELOR_INFORMATIK"), null, false);
        assertThat(new EducationLevelCondition("BACHELOR").isMet(ctx)).isTrue();
    }

    @Test
    void educationLevel_noMatch_false() {
        GameContext ctx = ctx(List.of("AUSBILDUNG_ELEKTRIKER"), null, false);
        assertThat(new EducationLevelCondition("BACHELOR").isMet(ctx)).isFalse();
    }

    // ── AllOfCondition ────────────────────────────────────────────────────────

    @Test
    void allOf_bothTrue_true() {
        GameContext ctx = ctx(List.of("CERT_A", "CERT_B"), null, false);
        Condition c = new AllOfCondition(new HasCertCondition("CERT_A"), new HasCertCondition("CERT_B"));
        assertThat(c.isMet(ctx)).isTrue();
    }

    @Test
    void allOf_onefalse_false() {
        GameContext ctx = ctx(List.of("CERT_A"), null, false);
        Condition c = new AllOfCondition(new HasCertCondition("CERT_A"), new HasCertCondition("CERT_B"));
        assertThat(c.isMet(ctx)).isFalse();
    }

    // ── AnyOfCondition ────────────────────────────────────────────────────────

    @Test
    void anyOf_oneTrue_true() {
        GameContext ctx = ctx(List.of("CERT_A"), null, false);
        Condition c = new AnyOfCondition(new HasCertCondition("CERT_A"), new HasCertCondition("CERT_B"));
        assertThat(c.isMet(ctx)).isTrue();
    }

    @Test
    void anyOf_noneTrue_false() {
        GameContext ctx = ctx(List.of(), null, false);
        Condition c = new AnyOfCondition(new HasCertCondition("CERT_A"), new HasCertCondition("CERT_B"));
        assertThat(c.isMet(ctx)).isFalse();
    }

    // ── NotCondition ──────────────────────────────────────────────────────────

    @Test
    void not_invertsFalseToTrue() {
        GameContext ctx = ctx(List.of(), null, false);
        assertThat(new NotCondition(new HasCertCondition("CERT_A")).isMet(ctx)).isTrue();
    }

    @Test
    void not_invertsTrueToFalse() {
        GameContext ctx = ctx(List.of("CERT_A"), null, false);
        assertThat(new NotCondition(new HasCertCondition("CERT_A")).isMet(ctx)).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private GameContext ctx(List<String> stages, GameCharacter character, boolean traveling) {
        return new GameContext(character, stages, null, traveling, 0, Set.of(), Map.of(), Set.of());
    }

    private GameCharacter characterWithSchufa(int score) {
        GameCharacter c = new GameCharacter();
        c.setSchufaScore(score);
        return c;
    }
}
