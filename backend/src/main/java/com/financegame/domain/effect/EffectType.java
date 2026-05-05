package com.financegame.domain.effect;

/**
 * Canonical registry of every multiplier, bonus, and stat-tick in the game.
 * All sources (social, collection, lifestyle) map their raw strings to this enum.
 * Adding a new effect: add an entry here, add a value in the source YAML/data,
 * implement the contributor value, add the consumer call-site. Zero other changes.
 */
public enum EffectType {

    // ── Percentage multipliers (value = fraction, e.g. 0.05 = 5%) ────────────

    /** income × (1 + v) */
    SALARY_MULTIPLIER,

    /** expenses × (1 – v) */
    EXPENSE_REDUCTION,

    /** annual loan rate (%) -= v × 100 */
    LOAN_INTEREST_REDUCTION,

    /** purchase price × (1 – v) */
    PROPERTY_PRICE_DISCOUNT,

    /** travel cost × (1 – v) */
    TRAVEL_COST_REDUCTION,

    /** travel months -= (int) v */
    TRAVEL_DURATION_REDUCTION,

    /** collectible price × (1 – v) */
    COLLECTIBLE_PRICE_DISCOUNT,

    /** detection chance × (1 – v) */
    TAX_DETECTION_REDUCTION,

    /** OU sigma × (1 – v) */
    STOCK_VOLATILITY_REDUCTION,

    /** job-acceptance probability += v */
    JOB_ACCEPTANCE_BOOST,

    /** gambling payout bias += v */
    GAMBLING_LUCK_BOOST,

    /** random collectible-event probability += v */
    COLLECTIBLE_DROP_RATE_BOOST,

    /** rob success probability += v */
    ROB_SUCCESS_BOOST,

    /** rob loot × (1 + v) */
    ROB_LOOT_MULTIPLIER,

    // ── Flat € modifiers ──────────────────────────────────────────────────────

    /** income += v (flat €) */
    SALARY_BONUS,

    /** income += v (flat €, additive with SALARY_BONUS) */
    MONTHLY_INCOME_BONUS,

    // ── Flat stat modifiers per turn ─────────────────────────────────────────

    /** hungerDecay -= v (flat points) */
    HUNGER_DECAY_REDUCTION,

    /** happiness += v */
    HAPPINESS_PER_TURN,

    /** stress -= v */
    STRESS_REDUCTION_PER_TURN,

    /** energy += v */
    ENERGY_BONUS_PER_TURN,

    /** schufaScore += v */
    SCHUFA_BONUS_MONTHLY,
}
