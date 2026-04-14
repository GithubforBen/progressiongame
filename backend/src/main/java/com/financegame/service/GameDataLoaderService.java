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
        int jobs = loadJobs();
        int collectibles = loadCollectibles();
        int properties = loadRealEstate();
        log.info("DataLoader: {} Jobs, {} Collectibles, {} Immobilien geladen", jobs, collectibles, properties);
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
            String requiredEducationType = (String) job.get("requiredEducationType");
            String requiredEducationField = (String) job.get("requiredEducationField");

            // Build JSON for OR-logic requirements if present
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
                // Use the first requirement as the legacy single-requirement fields if not set
                if (requiredEducationType == null && !eduReqs.isEmpty()) {
                    requiredEducationType = (String) eduReqs.get(0).get("type");
                    requiredEducationField = (String) eduReqs.get(0).get("field");
                }
            }

            em.createNativeQuery("""
                INSERT INTO jobs (name, description, required_education_type, required_education_field,
                    required_months_experience, salary, stress_per_month, education_requirements_json)
                VALUES (:name, :desc, :eduType, :eduField, :expMonths, :salary, :stress, :eduJson::jsonb)
                ON CONFLICT (name) DO UPDATE SET
                    description = EXCLUDED.description,
                    salary = EXCLUDED.salary,
                    stress_per_month = EXCLUDED.stress_per_month,
                    required_months_experience = EXCLUDED.required_months_experience,
                    education_requirements_json = EXCLUDED.education_requirements_json
                """)
                .setParameter("name", name)
                .setParameter("desc", description)
                .setParameter("eduType", requiredEducationType)
                .setParameter("eduField", requiredEducationField)
                .setParameter("expMonths", requiredMonthsExperience)
                .setParameter("salary", salary)
                .setParameter("stress", stressPerMonth)
                .setParameter("eduJson", eduReqJson)
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
                INSERT INTO collectibles (name, collection_type, country_required, rarity, base_value, description)
                VALUES (:name, :type, :country, :rarity, :value, :desc)
                ON CONFLICT (name) DO UPDATE SET
                    collection_type = EXCLUDED.collection_type,
                    country_required = EXCLUDED.country_required,
                    rarity = EXCLUDED.rarity,
                    base_value = EXCLUDED.base_value,
                    description = EXCLUDED.description
                """)
                .setParameter("name", item.get("name"))
                .setParameter("type", item.get("collectionType"))
                .setParameter("country", item.get("countryRequired"))
                .setParameter("rarity", item.get("rarity"))
                .setParameter("value", toDouble(item.get("baseValue")))
                .setParameter("desc", item.get("description"))
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
                    purchase_price, monthly_rent, rent_savings)
                VALUES (:name, :location, :category, :desc, :price, :rent, :savings)
                ON CONFLICT (name) DO UPDATE SET
                    location = EXCLUDED.location,
                    category = EXCLUDED.category,
                    description = EXCLUDED.description,
                    purchase_price = EXCLUDED.purchase_price,
                    monthly_rent = EXCLUDED.monthly_rent,
                    rent_savings = EXCLUDED.rent_savings
                """)
                .setParameter("name", prop.get("name"))
                .setParameter("location", prop.get("location"))
                .setParameter("category", prop.get("category"))
                .setParameter("desc", prop.get("description"))
                .setParameter("price", toDouble(prop.get("purchasePrice")))
                .setParameter("rent", toDouble(prop.get("monthlyRent")))
                .setParameter("savings", toDouble(prop.get("rentSavings")))
                .executeUpdate();
        }
        return properties.size();
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
