package com.financegame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All tunable game constants — loaded from application.yml under the "game:" key.
 * Changing a value requires only a config change, not a code recompile.
 */
@Component
@ConfigurationProperties(prefix = "game")
public class GameConfig {

    private Map<String, Double> stockVolatility = new HashMap<>();
    private Map<String, Double> stockReversionSpeed = new HashMap<>();
    private StockDelistingConfig stockDelisting = new StockDelistingConfig();
    private NeedsConfig needs = new NeedsConfig();
    private BurnoutConfig burnout = new BurnoutConfig();
    private DepressionConfig depression = new DepressionConfig();
    private LoanConfig loan = new LoanConfig();
    private SchufaConfig schufa = new SchufaConfig();
    private TaxEvasionConfig taxEvasion = new TaxEvasionConfig();
    private TaxConfig tax = new TaxConfig();

    // -------------------------------------------------------------------------
    // Nested config classes
    // -------------------------------------------------------------------------

    public static class StockDelistingConfig {
        private double thresholdFraction = 0.10;
        private int minDelistTurns = 6;
        private double relistChancePerTurn = 0.20;

        public double getThresholdFraction() { return thresholdFraction; }
        public void setThresholdFraction(double v) { this.thresholdFraction = v; }
        public int getMinDelistTurns() { return minDelistTurns; }
        public void setMinDelistTurns(int v) { this.minDelistTurns = v; }
        public double getRelistChancePerTurn() { return relistChancePerTurn; }
        public void setRelistChancePerTurn(double v) { this.relistChancePerTurn = v; }
    }

    public static class NeedsConfig {
        private int hungerDecayBase = 15;
        private int hungerDecayWithFood = 5;
        private int energyDecay = 8;
        private int happinessDecay = 5;

        public int getHungerDecayBase() { return hungerDecayBase; }
        public void setHungerDecayBase(int v) { this.hungerDecayBase = v; }
        public int getHungerDecayWithFood() { return hungerDecayWithFood; }
        public void setHungerDecayWithFood(int v) { this.hungerDecayWithFood = v; }
        public int getEnergyDecay() { return energyDecay; }
        public void setEnergyDecay(int v) { this.energyDecay = v; }
        public int getHappinessDecay() { return happinessDecay; }
        public void setHappinessDecay(int v) { this.happinessDecay = v; }
    }

    public static class BurnoutConfig {
        private int stressTrigger = 100;
        private int stressReset = 50;
        private int recoveryThreshold = 40;
        private int hospitalPenalty = 100;

        public int getStressTrigger() { return stressTrigger; }
        public void setStressTrigger(int v) { this.stressTrigger = v; }
        public int getStressReset() { return stressReset; }
        public void setStressReset(int v) { this.stressReset = v; }
        public int getRecoveryThreshold() { return recoveryThreshold; }
        public void setRecoveryThreshold(int v) { this.recoveryThreshold = v; }
        public int getHospitalPenalty() { return hospitalPenalty; }
        public void setHospitalPenalty(int v) { this.hospitalPenalty = v; }
    }

    public static class DepressionConfig {
        private int durationMonths = 3;
        private int stressPerMonth = 3;

        public int getDurationMonths() { return durationMonths; }
        public void setDurationMonths(int v) { this.durationMonths = v; }
        public int getStressPerMonth() { return stressPerMonth; }
        public void setStressPerMonth(int v) { this.stressPerMonth = v; }
    }

    public static class LoanConfig {
        private int minSchufa = 300;
        private int minAmount = 1000;
        private List<InterestTier> interestTiers = new ArrayList<>();

        public int getMinSchufa() { return minSchufa; }
        public void setMinSchufa(int v) { this.minSchufa = v; }
        public int getMinAmount() { return minAmount; }
        public void setMinAmount(int v) { this.minAmount = v; }
        public List<InterestTier> getInterestTiers() { return interestTiers; }
        public void setInterestTiers(List<InterestTier> v) { this.interestTiers = v; }

        public static class InterestTier {
            private int minScore;
            private double rate;
            private String label;

            public int getMinScore() { return minScore; }
            public void setMinScore(int v) { this.minScore = v; }
            public double getRate() { return rate; }
            public void setRate(double v) { this.rate = v; }
            public String getLabel() { return label; }
            public void setLabel(String v) { this.label = v; }
        }
    }

    public static class SchufaConfig {
        /** Map from education stage prefix (e.g. "MASTER") to SCHUFA bonus points. */
        private Map<String, Integer> educationBonuses = new HashMap<>();

        public Map<String, Integer> getEducationBonuses() { return educationBonuses; }
        public void setEducationBonuses(Map<String, Integer> v) { this.educationBonuses = v; }
    }

    public static class TaxEvasionConfig {
        private List<TaxEvasionLevel> levels = new ArrayList<>();

        public List<TaxEvasionLevel> getLevels() { return levels; }
        public void setLevels(List<TaxEvasionLevel> v) { this.levels = v; }

        public static class TaxEvasionLevel {
            private String certSuffix;
            private double evasionRate;
            private double detectionChance;

            public String getCertSuffix() { return certSuffix; }
            public void setCertSuffix(String v) { this.certSuffix = v; }
            public double getEvasionRate() { return evasionRate; }
            public void setEvasionRate(double v) { this.evasionRate = v; }
            public double getDetectionChance() { return detectionChance; }
            public void setDetectionChance(double v) { this.detectionChance = v; }
        }
    }

    public static class TaxConfig {
        private List<TaxBracket> brackets = new ArrayList<>();

        public List<TaxBracket> getBrackets() { return brackets; }
        public void setBrackets(List<TaxBracket> v) { this.brackets = v; }

        public static class TaxBracket {
            private double upTo;
            private double rate;
            private String label;

            public double getUpTo() { return upTo; }
            public void setUpTo(double v) { this.upTo = v; }
            public double getRate() { return rate; }
            public void setRate(double v) { this.rate = v; }
            public String getLabel() { return label; }
            public void setLabel(String v) { this.label = v; }
        }
    }

    // -------------------------------------------------------------------------
    // Root getters/setters
    // -------------------------------------------------------------------------

    public Map<String, Double> getStockVolatility() { return stockVolatility; }
    public void setStockVolatility(Map<String, Double> v) { this.stockVolatility = v; }

    public Map<String, Double> getStockReversionSpeed() { return stockReversionSpeed; }
    public void setStockReversionSpeed(Map<String, Double> v) { this.stockReversionSpeed = v; }

    public StockDelistingConfig getStockDelisting() { return stockDelisting; }
    public void setStockDelisting(StockDelistingConfig v) { this.stockDelisting = v; }

    public NeedsConfig getNeeds() { return needs; }
    public void setNeeds(NeedsConfig v) { this.needs = v; }

    public BurnoutConfig getBurnout() { return burnout; }
    public void setBurnout(BurnoutConfig v) { this.burnout = v; }

    public DepressionConfig getDepression() { return depression; }
    public void setDepression(DepressionConfig v) { this.depression = v; }

    public LoanConfig getLoan() { return loan; }
    public void setLoan(LoanConfig v) { this.loan = v; }

    public SchufaConfig getSchufa() { return schufa; }
    public void setSchufa(SchufaConfig v) { this.schufa = v; }

    public TaxEvasionConfig getTaxEvasion() { return taxEvasion; }
    public void setTaxEvasion(TaxEvasionConfig v) { this.taxEvasion = v; }

    public TaxConfig getTax() { return tax; }
    public void setTax(TaxConfig v) { this.tax = v; }
}
