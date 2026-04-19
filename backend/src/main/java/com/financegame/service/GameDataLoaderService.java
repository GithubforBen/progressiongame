package com.financegame.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class GameDataLoaderService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GameDataLoaderService.class);

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        int collections = loadCollections();
        int jobs = loadJobs();
        int collectibles = loadCollectibles();
        int properties = loadRealEstate();
        int needsItems = loadNeedsItems();
        int stocks = loadStocks();
        log.info("DataLoader: {} Collections, {} Jobs, {} Collectibles, {} Immobilien, {} NeedsItems, {} Stocks geladen",
            collections, jobs, collectibles, properties, needsItems, stocks);
    }

    @SuppressWarnings("unchecked")
    private int loadCollections() {
        InputStream is = getClass().getResourceAsStream("/data/collections.yaml");
        if (is == null) { log.warn("collections.yaml nicht gefunden"); return 0; }

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(is);
        List<Map<String, Object>> collections = (List<Map<String, Object>>) data.get("collections");
        if (collections == null) return 0;

        for (Map<String, Object> col : collections) {
            em.createNativeQuery("""
                INSERT INTO collections (name, display_name, bonus_type, bonus_value, item_count, required_cert)
                VALUES (:name, :displayName, :bonusType, :bonusValue, :itemCount, :requiredCert)
                ON CONFLICT (name) DO UPDATE SET
                    display_name  = EXCLUDED.display_name,
                    bonus_type    = EXCLUDED.bonus_type,
                    bonus_value   = EXCLUDED.bonus_value,
                    item_count    = EXCLUDED.item_count,
                    required_cert = EXCLUDED.required_cert
                """)
                .setParameter("name", col.get("name"))
                .setParameter("displayName", col.get("displayName"))
                .setParameter("bonusType", col.get("bonusType"))
                .setParameter("bonusValue", toDouble(col.get("bonusValue")))
                .setParameter("itemCount", toInt(col.get("itemCount")))
                .setParameter("requiredCert", col.get("requiredCert"))
                .executeUpdate();
        }
        return collections.size();
    }

    @SuppressWarnings("unchecked")
    private int loadJobs() {
        InputStream is = getClass().getResourceAsStream("/data/jobs.yaml");
        if (is == null) { log.warn("jobs.yaml nicht gefunden"); return 0; }

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(is);
        List<Map<String, Object>> jobs = (List<Map<String, Object>>) data.get("jobs");
        if (jobs == null) return 0;

        for (Map<String, Object> job : jobs) {
            String name = (String) job.get("name");
            String description = (String) job.get("description");
            double salary = toDouble(job.get("salary"));
            int stressPerMonth = toInt(job.get("stressPerMonth"));
            int requiredMonthsExperience = toInt(job.get("requiredMonthsExperience"));
            String category = (String) job.getOrDefault("category", "EINSTIEG");
            int maxParallel = toInt(job.getOrDefault("maxParallel", 1));
            String requiredSideCert = (String) job.get("requiredSideCert");

            // requiredStageKey stores the full completed-stage key (e.g. "AUSBILDUNG_EINZELHANDEL")
            // directly in required_education_type with no required_education_field.
            String requiredStageKey = (String) job.get("requiredStageKey");
            String requiredEducationType = requiredStageKey != null ? requiredStageKey
                : (String) job.get("requiredEducationType");
            String requiredEducationField = requiredStageKey != null ? null
                : (String) job.get("requiredEducationField");

            // Build JSON for OR-logic requirements if present (legacy, not used in new catalog)
            List<Map<String, Object>> eduReqs = (List<Map<String, Object>>) job.get("educationRequirements");
            String eduReqJson = null;
            if (eduReqs != null && !eduReqs.isEmpty()) {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < eduReqs.size(); i++) {
                    Map<String, Object> req = eduReqs.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"type\":\"").append(req.get("type")).append("\"");
                    if (req.get("field") != null) sb.append(",\"field\":\"").append(req.get("field")).append("\"");
                    if (req.get("minExperience") != null) sb.append(",\"minExperience\":").append(req.get("minExperience"));
                    sb.append("}");
                }
                sb.append("]");
                eduReqJson = sb.toString();
                if (requiredEducationType == null && !eduReqs.isEmpty()) {
                    requiredEducationType = (String) eduReqs.get(0).get("type");
                    requiredEducationField = (String) eduReqs.get(0).get("field");
                }
            }

            em.createNativeQuery("""
                INSERT INTO jobs (name, description, required_education_type, required_education_field,
                    required_months_experience, salary, stress_per_month, education_requirements_json,
                    category, max_parallel, required_side_cert, available)
                VALUES (:name, :desc, :eduType, :eduField, :expMonths, :salary, :stress, CAST(:eduJson AS jsonb),
                    :category, :maxParallel, :sideCert, true)
                ON CONFLICT (name) DO UPDATE SET
                    description             = EXCLUDED.description,
                    salary                  = EXCLUDED.salary,
                    stress_per_month        = EXCLUDED.stress_per_month,
                    required_education_type = EXCLUDED.required_education_type,
                    required_education_field= EXCLUDED.required_education_field,
                    required_months_experience = EXCLUDED.required_months_experience,
                    education_requirements_json = EXCLUDED.education_requirements_json,
                    category                = EXCLUDED.category,
                    max_parallel            = EXCLUDED.max_parallel,
                    required_side_cert      = EXCLUDED.required_side_cert,
                    available               = true
                """)
                .setParameter("name", name)
                .setParameter("desc", description)
                .setParameter("eduType", requiredEducationType)
                .setParameter("eduField", requiredEducationField)
                .setParameter("expMonths", requiredMonthsExperience)
                .setParameter("salary", salary)
                .setParameter("stress", stressPerMonth)
                .setParameter("eduJson", eduReqJson)
                .setParameter("category", category)
                .setParameter("maxParallel", maxParallel)
                .setParameter("sideCert", requiredSideCert)
                .executeUpdate();
        }
        return jobs.size();
    }

    @SuppressWarnings("unchecked")
    private int loadCollectibles() {
        InputStream is = getClass().getResourceAsStream("/data/collectibles.yaml");
        if (is == null) { log.warn("collectibles.yaml nicht gefunden"); return 0; }

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(is);
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("collectibles");
        if (items == null) return 0;

        for (Map<String, Object> item : items) {
            em.createNativeQuery("""
                INSERT INTO collectibles (name, collection_type, country_required, rarity, base_value, description,
                    collection_name, price)
                VALUES (:name, :type, :country, :rarity, :value, :desc, :colName, :price)
                ON CONFLICT (name) DO UPDATE SET
                    collection_type  = EXCLUDED.collection_type,
                    country_required = EXCLUDED.country_required,
                    rarity           = EXCLUDED.rarity,
                    base_value       = EXCLUDED.base_value,
                    description      = EXCLUDED.description,
                    collection_name  = EXCLUDED.collection_name,
                    price            = EXCLUDED.price
                """)
                .setParameter("name", item.get("name"))
                .setParameter("type", item.get("collectionType"))
                .setParameter("country", item.get("countryRequired"))
                .setParameter("rarity", item.get("rarity"))
                .setParameter("value", toDouble(item.get("baseValue")))
                .setParameter("desc", item.get("description"))
                .setParameter("colName", item.get("collectionName"))
                .setParameter("price", toDouble(item.get("price")))
                .executeUpdate();
        }
        return items.size();
    }

    @SuppressWarnings("unchecked")
    private int loadRealEstate() {
        InputStream is = getClass().getResourceAsStream("/data/real_estate.yaml");
        if (is == null) { log.warn("real_estate.yaml nicht gefunden"); return 0; }

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(is);
        List<Map<String, Object>> properties = (List<Map<String, Object>>) data.get("properties");
        if (properties == null) return 0;

        for (Map<String, Object> prop : properties) {
            em.createNativeQuery("""
                INSERT INTO real_estate_catalog (name, location, category, description,
                    purchase_price, monthly_rent, rent_savings, required_cert)
                VALUES (:name, :location, :category, :desc, :price, :rent, :savings, :requiredCert)
                ON CONFLICT (name) DO UPDATE SET
                    location      = EXCLUDED.location,
                    category      = EXCLUDED.category,
                    description   = EXCLUDED.description,
                    purchase_price= EXCLUDED.purchase_price,
                    monthly_rent  = EXCLUDED.monthly_rent,
                    rent_savings  = EXCLUDED.rent_savings,
                    required_cert = EXCLUDED.required_cert
                """)
                .setParameter("name", prop.get("name"))
                .setParameter("location", prop.get("location"))
                .setParameter("category", prop.get("category"))
                .setParameter("desc", prop.get("description"))
                .setParameter("price", toDouble(prop.get("purchasePrice")))
                .setParameter("rent", toDouble(prop.get("monthlyRent")))
                .setParameter("savings", toDouble(prop.get("rentSavings")))
                .setParameter("requiredCert", prop.get("requiredCert"))
                .executeUpdate();
        }
        return properties.size();
    }

    @SuppressWarnings("unchecked")
    private int loadNeedsItems() {
        InputStream is = getClass().getResourceAsStream("/data/needs_items.yaml");
        if (is == null) { log.warn("needs_items.yaml nicht gefunden"); return 0; }

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(is);
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("needsItems");
        if (items == null) return 0;

        for (Map<String, Object> item : items) {
            em.createNativeQuery("""
                INSERT INTO needs_items (id, name, price, hunger_effect, energy_effect,
                    happiness_effect, stress_effect, depression_reduction)
                VALUES (:id, :name, :price, :hunger, :energy, :happiness, :stress, :depress)
                ON CONFLICT (id) DO UPDATE SET
                    name                = EXCLUDED.name,
                    price               = EXCLUDED.price,
                    hunger_effect       = EXCLUDED.hunger_effect,
                    energy_effect       = EXCLUDED.energy_effect,
                    happiness_effect    = EXCLUDED.happiness_effect,
                    stress_effect       = EXCLUDED.stress_effect,
                    depression_reduction= EXCLUDED.depression_reduction
                """)
                .setParameter("id", item.get("id"))
                .setParameter("name", item.get("name"))
                .setParameter("price", toDouble(item.get("price")))
                .setParameter("hunger", toInt(item.get("hungerEffect")))
                .setParameter("energy", toInt(item.get("energyEffect")))
                .setParameter("happiness", toInt(item.get("happinessEffect")))
                .setParameter("stress", toInt(item.get("stressEffect")))
                .setParameter("depress", Boolean.TRUE.equals(item.get("depressionReduction")))
                .executeUpdate();
        }
        return items.size();
    }

    @SuppressWarnings("unchecked")
    private int loadStocks() {
        InputStream is = getClass().getResourceAsStream("/data/stocks.yaml");
        if (is == null) { log.warn("stocks.yaml nicht gefunden"); return 0; }

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(is);
        List<Map<String, Object>> stocks = (List<Map<String, Object>>) data.get("stocks");
        if (stocks == null) return 0;

        for (Map<String, Object> stock : stocks) {
            em.createNativeQuery("""
                INSERT INTO stocks (name, ticker, type, current_price, required_cert)
                VALUES (:name, :ticker, :type, :price, :cert)
                ON CONFLICT (ticker) DO UPDATE SET
                    name          = EXCLUDED.name,
                    type          = EXCLUDED.type,
                    required_cert = EXCLUDED.required_cert
                """)
                .setParameter("name", stock.get("name"))
                .setParameter("ticker", stock.get("ticker"))
                .setParameter("type", stock.get("type"))
                .setParameter("price", toDouble(stock.get("currentPrice")))
                .setParameter("cert", stock.get("requiredCert"))
                .executeUpdate();
        }
        return stocks.size();
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        return Integer.parseInt(val.toString());
    }
}
