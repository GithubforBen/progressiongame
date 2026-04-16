package com.financegame.service;

import com.financegame.dto.EducationProgressDto;
import com.financegame.dto.EducationProgressDto.*;
import com.financegame.dto.EnrollMainRequest;
import com.financegame.dto.EnrollSideRequest;
import com.financegame.entity.EducationProgress;
import com.financegame.repository.EducationProgressRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
public class EducationService {

    private static final Logger log = LoggerFactory.getLogger(EducationService.class);

    /** Internal definition of a main education stage. */
    record StageDefinition(
        String parentKey,
        int durationMonths,
        boolean requiresField,
        List<FieldOption> fieldOptions,
        int cost,
        /** Per-field duration overrides (field value → months). Empty = all fields use durationMonths. */
        Map<String, Integer> fieldDurations
    ) {}

    /** Internal definition of a side certification. */
    record SideCertDef(
        String certKey,
        String label,
        int durationMonths,
        int cost,
        /** OR-logic prerequisites: player must have at least one. Empty list = no requirement. */
        List<String> requiresAny
    ) {}

    // Populated from YAML on startup; fallback to static data if loading fails
    private final Map<String, StageDefinition> mainStages = new LinkedHashMap<>();
    private final Map<String, SideCertDef> sideCerts = new LinkedHashMap<>();

    private final EducationProgressRepository educationProgressRepository;
    private final CharacterService characterService;

    public EducationService(EducationProgressRepository educationProgressRepository,
                            CharacterService characterService) {
        this.educationProgressRepository = educationProgressRepository;
        this.characterService = characterService;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void loadEducationData() {
        try {
            InputStream is = getClass().getResourceAsStream("/data/education.yaml");
            if (is == null) {
                log.warn("education.yaml nicht gefunden – nutze Standarddaten");
                loadDefaults();
                return;
            }
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);

            List<Map<String, Object>> stages = (List<Map<String, Object>>) data.get("mainStages");
            if (stages != null) {
                for (Map<String, Object> s : stages) {
                    String key = (String) s.get("key");
                    String parentKey = (String) s.get("parentKey");
                    int duration = toInt(s.get("durationMonths"));
                    boolean requiresField = Boolean.TRUE.equals(s.get("requiresField"));
                    int cost = toInt(s.get("cost"));

                    List<FieldOption> fields = new ArrayList<>();
                    Map<String, Integer> fieldDurations = new HashMap<>();

                    List<Map<String, Object>> fieldList = (List<Map<String, Object>>) s.get("fields");
                    if (fieldList != null) {
                        for (Map<String, Object> f : fieldList) {
                            String fValue = (String) f.get("value");
                            String fLabel = (String) f.get("label");
                            int fDuration = toInt(f.get("durationMonths")); // 0 = use stage default
                            int effectiveDuration = fDuration > 0 ? fDuration : duration;
                            fields.add(new FieldOption(fValue, fLabel, effectiveDuration));
                            if (fDuration > 0) fieldDurations.put(fValue, fDuration);
                        }
                    }
                    mainStages.put(key, new StageDefinition(parentKey, duration, requiresField,
                        fields, cost, fieldDurations));
                }
            }

            // Support both legacy "sideCerts" flat list and new "sideCertFamilies" multi-level list
            List<Map<String, Object>> legacyCerts = (List<Map<String, Object>>) data.get("sideCerts");
            if (legacyCerts != null) {
                for (Map<String, Object> c : legacyCerts) {
                    String key = (String) c.get("key");
                    String label = (String) c.get("label");
                    int duration = toInt(c.get("durationMonths"));
                    int cost = toInt(c.get("cost"));
                    List<String> requiresAny = parseRequiresAny(c);
                    sideCerts.put(key, new SideCertDef(key, label, duration, cost, requiresAny));
                }
            }

            List<Map<String, Object>> families = (List<Map<String, Object>>) data.get("sideCertFamilies");
            if (families != null) {
                for (Map<String, Object> family : families) {
                    List<Map<String, Object>> levels = (List<Map<String, Object>>) family.get("levels");
                    if (levels == null) continue;
                    for (Map<String, Object> lvl : levels) {
                        String certKey = (String) lvl.get("certKey");
                        String label = (String) lvl.get("label");
                        int duration = toInt(lvl.get("durationMonths"));
                        int cost = toInt(lvl.get("cost"));
                        List<String> requiresAny = parseRequiresAny(lvl);
                        // Use full certKey as map key (e.g. "WEITERBILDUNG_SOCIAL_MEDIA_1")
                        sideCerts.put(certKey, new SideCertDef(certKey, label, duration, cost, requiresAny));
                    }
                }
            }
            log.info("EducationService: {} Stufen, {} Zertifikate aus YAML geladen",
                mainStages.size(), sideCerts.size());
        } catch (Exception e) {
            log.error("Fehler beim Laden von education.yaml: {}", e.getMessage());
            loadDefaults();
        }
    }

    private void loadDefaults() {
        mainStages.clear();
        sideCerts.clear();
        mainStages.put("REALSCHULABSCHLUSS", new StageDefinition("GRUNDSCHULE", 2, false,
            List.of(), 0, Map.of()));
        mainStages.put("ABITUR", new StageDefinition("REALSCHULABSCHLUSS", 3, false,
            List.of(), 0, Map.of()));
        mainStages.put("AUSBILDUNG", new StageDefinition("REALSCHULABSCHLUSS", 4, true, List.of(
            new FieldOption("FACHINFORMATIKER", "Fachinformatiker/-in", 4),
            new FieldOption("EINZELHANDEL",    "Einzelhandelskaufmann/-frau", 4),
            new FieldOption("ELEKTRIKER",      "Elektriker/-in", 4),
            new FieldOption("PFLEGE",          "Pflegefachkraft", 4)
        ), 500, Map.of()));
        mainStages.put("BACHELOR", new StageDefinition("ABITUR", 6, true, List.of(
            new FieldOption("INFORMATIK",    "Informatik (B.Sc.)", 6),
            new FieldOption("BWL",           "Betriebswirtschaft (B.Sc.)", 6),
            new FieldOption("MEDIZIN",       "Medizin (Staatsexamen)", 8),
            new FieldOption("JURA",          "Rechtswissenschaften (1. Staatsexamen)", 7),
            new FieldOption("INGENIEURWESEN","Ingenieurwesen (B.Eng.)", 6),
            new FieldOption("PSYCHOLOGIE",   "Psychologie (B.Sc.)", 6)
        ), 3000, Map.of("MEDIZIN", 8, "JURA", 7)));
        mainStages.put("MASTER", new StageDefinition("BACHELOR", 4, true, List.of(
            new FieldOption("INFORMATIK",    "Informatik (M.Sc.)", 4),
            new FieldOption("BWL",           "Betriebswirtschaft (MBA)", 4),
            new FieldOption("MEDIZIN",       "Medizin (Approbation)", 4),
            new FieldOption("JURA",          "Rechtswissenschaften (2. Staatsexamen)", 4),
            new FieldOption("INGENIEURWESEN","Ingenieurwesen (M.Eng.)", 4)
        ), 5000, Map.of()));
        sideCerts.put("WEITERBILDUNG_BARKEEPER_1",          new SideCertDef("WEITERBILDUNG_BARKEEPER_1",          "Barkeeper-Kurs (Grundlagen)",              1, 250,  List.of()));
        sideCerts.put("WEITERBILDUNG_BARKEEPER_2",          new SideCertDef("WEITERBILDUNG_BARKEEPER_2",          "Barkeeper-Kurs (Fortgeschritten)",         1, 500,  List.of("WEITERBILDUNG_BARKEEPER_1")));
        sideCerts.put("WEITERBILDUNG_BARKEEPER_3",          new SideCertDef("WEITERBILDUNG_BARKEEPER_3",          "Bar-Manager Zertifikat",                   2, 900,  List.of("WEITERBILDUNG_BARKEEPER_2")));
        sideCerts.put("WEITERBILDUNG_FITNESSTRAINER_1",     new SideCertDef("WEITERBILDUNG_FITNESSTRAINER_1",     "Fitnesstrainer B-Lizenz",                  1, 400,  List.of()));
        sideCerts.put("WEITERBILDUNG_FITNESSTRAINER_2",     new SideCertDef("WEITERBILDUNG_FITNESSTRAINER_2",     "Fitnesstrainer A-Lizenz",                  2, 700,  List.of("WEITERBILDUNG_FITNESSTRAINER_1")));
        sideCerts.put("WEITERBILDUNG_FITNESSTRAINER_3",     new SideCertDef("WEITERBILDUNG_FITNESSTRAINER_3",     "Personal Trainer Zertifikat",              2, 1200, List.of("WEITERBILDUNG_FITNESSTRAINER_2")));
        sideCerts.put("WEITERBILDUNG_SOCIAL_MEDIA_1",       new SideCertDef("WEITERBILDUNG_SOCIAL_MEDIA_1",       "Social-Media-Marketing (Grundkurs)",       1, 300,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_SOCIAL_MEDIA_2",       new SideCertDef("WEITERBILDUNG_SOCIAL_MEDIA_2",       "Social-Media-Marketing (Aufbaukurs)",      2, 600,  List.of("WEITERBILDUNG_SOCIAL_MEDIA_1")));
        sideCerts.put("WEITERBILDUNG_SOCIAL_MEDIA_3",       new SideCertDef("WEITERBILDUNG_SOCIAL_MEDIA_3",       "Social-Media-Marketing (Expertenzertifikat)", 2, 1100, List.of("WEITERBILDUNG_SOCIAL_MEDIA_2")));
        sideCerts.put("WEITERBILDUNG_EXCEL_1",              new SideCertDef("WEITERBILDUNG_EXCEL_1",              "Excel & Datenanalyse (Grundkurs)",         1, 150,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_EXCEL_2",              new SideCertDef("WEITERBILDUNG_EXCEL_2",              "Excel & Power BI (Aufbaukurs)",            1, 350,  List.of("WEITERBILDUNG_EXCEL_1")));
        sideCerts.put("WEITERBILDUNG_EXCEL_3",              new SideCertDef("WEITERBILDUNG_EXCEL_3",              "Data Analyst Zertifikat (Microsoft)",      2, 800,  List.of("WEITERBILDUNG_EXCEL_2")));
        sideCerts.put("WEITERBILDUNG_FUEHRERSCHEIN_1",      new SideCertDef("WEITERBILDUNG_FUEHRERSCHEIN_1",      "Führerschein Klasse B",                    1, 1800, List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_FUEHRERSCHEIN_2",      new SideCertDef("WEITERBILDUNG_FUEHRERSCHEIN_2",      "Führerschein Klasse BE + Anhänger",        1, 600,  List.of("WEITERBILDUNG_FUEHRERSCHEIN_1")));
        sideCerts.put("WEITERBILDUNG_FUEHRERSCHEIN_3",      new SideCertDef("WEITERBILDUNG_FUEHRERSCHEIN_3",      "LKW-Führerschein Klasse C+E",              2, 3500, List.of("WEITERBILDUNG_FUEHRERSCHEIN_2")));
        sideCerts.put("WEITERBILDUNG_CRYPTO_1",             new SideCertDef("WEITERBILDUNG_CRYPTO_1",             "Krypto-Trading Zertifikat (Grundlagen)",   1, 400,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_CRYPTO_2",             new SideCertDef("WEITERBILDUNG_CRYPTO_2",             "DeFi & Blockchain Zertifikat",             2, 800,  List.of("WEITERBILDUNG_CRYPTO_1")));
        sideCerts.put("WEITERBILDUNG_CRYPTO_3",             new SideCertDef("WEITERBILDUNG_CRYPTO_3",             "Certified Crypto Analyst (CCA)",           2, 1500, List.of("WEITERBILDUNG_CRYPTO_2")));
        sideCerts.put("WEITERBILDUNG_BUCHHALTUNG_1",        new SideCertDef("WEITERBILDUNG_BUCHHALTUNG_1",        "Buchhaltung & DATEV (Grundkurs)",          1, 350,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_BUCHHALTUNG_2",        new SideCertDef("WEITERBILDUNG_BUCHHALTUNG_2",        "Bilanzbuchhaltung (IHK)",                  2, 700,  List.of("WEITERBILDUNG_BUCHHALTUNG_1")));
        sideCerts.put("WEITERBILDUNG_BUCHHALTUNG_3",        new SideCertDef("WEITERBILDUNG_BUCHHALTUNG_3",        "Bilanzbuchhalter Zertifikat",              3, 1400, List.of("WEITERBILDUNG_BUCHHALTUNG_2")));
        sideCerts.put("WEITERBILDUNG_IMMOBILIEN_1",         new SideCertDef("WEITERBILDUNG_IMMOBILIEN_1",         "Immobilien-Grundlagen (IHK)",              1, 600,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_IMMOBILIEN_2",         new SideCertDef("WEITERBILDUNG_IMMOBILIEN_2",         "Immobilienmakler-Lizenz",                  2, 1200, List.of("WEITERBILDUNG_IMMOBILIEN_1")));
        sideCerts.put("WEITERBILDUNG_IMMOBILIEN_3",         new SideCertDef("WEITERBILDUNG_IMMOBILIEN_3",         "Immobilien-Investor Masterclass",          2, 2500, List.of("WEITERBILDUNG_IMMOBILIEN_2")));
        sideCerts.put("WEITERBILDUNG_PROJEKTMANAGEMENT_1",  new SideCertDef("WEITERBILDUNG_PROJEKTMANAGEMENT_1",  "Projektmanagement Grundlagen (PMI)",       1, 400,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_PROJEKTMANAGEMENT_2",  new SideCertDef("WEITERBILDUNG_PROJEKTMANAGEMENT_2",  "Projektmanagement (PRINCE2 Foundation)",   2, 800,  List.of("WEITERBILDUNG_PROJEKTMANAGEMENT_1")));
        sideCerts.put("WEITERBILDUNG_PROJEKTMANAGEMENT_3",  new SideCertDef("WEITERBILDUNG_PROJEKTMANAGEMENT_3",  "PMP Zertifizierung",                       2, 1800, List.of("WEITERBILDUNG_PROJEKTMANAGEMENT_2", "BACHELOR_BWL", "BACHELOR_INFORMATIK")));
        sideCerts.put("WEITERBILDUNG_STEUERN_1",            new SideCertDef("WEITERBILDUNG_STEUERN_1",            "Steuerlehre Grundkurs",                    1, 300,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_STEUERN_2",            new SideCertDef("WEITERBILDUNG_STEUERN_2",            "Steuerberater-Vorbereitung",               2, 600,  List.of("WEITERBILDUNG_STEUERN_1", "BACHELOR_BWL")));
        sideCerts.put("WEITERBILDUNG_STEUERN_3",            new SideCertDef("WEITERBILDUNG_STEUERN_3",            "Steuerberater-Examen",                     3, 2000, List.of("WEITERBILDUNG_STEUERN_2")));
        sideCerts.put("WEITERBILDUNG_HACKER_1",             new SideCertDef("WEITERBILDUNG_HACKER_1",             "IT-Security Grundlagen",                   1, 450,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_HACKER_2",             new SideCertDef("WEITERBILDUNG_HACKER_2",             "Ethical Hacking Zertifikat (CEH)",         2, 900,  List.of("WEITERBILDUNG_HACKER_1", "BACHELOR_INFORMATIK", "AUSBILDUNG_FACHINFORMATIKER")));
        sideCerts.put("WEITERBILDUNG_HACKER_3",             new SideCertDef("WEITERBILDUNG_HACKER_3",             "OSCP Penetration Testing",                 3, 2200, List.of("WEITERBILDUNG_HACKER_2")));
        sideCerts.put("WEITERBILDUNG_IMMOBILIEN_4",         new SideCertDef("WEITERBILDUNG_IMMOBILIEN_4",         "Immobilien-Portfolio Manager",             3, 4000, List.of("WEITERBILDUNG_IMMOBILIEN_3", "MASTER_BWL")));
        sideCerts.put("WEITERBILDUNG_OLDTIMER_1",           new SideCertDef("WEITERBILDUNG_OLDTIMER_1",           "Oldtimer-Kurs Grundlagen",                 1, 400,  List.of("WEITERBILDUNG_FUEHRERSCHEIN_1")));
        sideCerts.put("WEITERBILDUNG_OLDTIMER_2",           new SideCertDef("WEITERBILDUNG_OLDTIMER_2",           "Classic-Car Experte",                      2, 900,  List.of("WEITERBILDUNG_OLDTIMER_1")));
        sideCerts.put("WEITERBILDUNG_OLDTIMER_3",           new SideCertDef("WEITERBILDUNG_OLDTIMER_3",           "Oldtimer-Auktionator Zertifikat",          2, 1800, List.of("WEITERBILDUNG_OLDTIMER_2")));
        sideCerts.put("WEITERBILDUNG_ARCHAEOLOGIE_1",       new SideCertDef("WEITERBILDUNG_ARCHAEOLOGIE_1",       "Archäologen-Hobbykurs",                    1, 300,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_ARCHAEOLOGIE_2",       new SideCertDef("WEITERBILDUNG_ARCHAEOLOGIE_2",       "Antiquitäten-Experte",                     2, 700,  List.of("WEITERBILDUNG_ARCHAEOLOGIE_1")));
        sideCerts.put("WEITERBILDUNG_WEINKENNER_1",         new SideCertDef("WEITERBILDUNG_WEINKENNER_1",         "Weinkenner Grundkurs",                     1, 350,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_WEINKENNER_2",         new SideCertDef("WEITERBILDUNG_WEINKENNER_2",         "Wine & Spirit Education (WSET)",           2, 800,  List.of("WEITERBILDUNG_WEINKENNER_1")));
        sideCerts.put("WEITERBILDUNG_WEINKENNER_3",         new SideCertDef("WEITERBILDUNG_WEINKENNER_3",         "Master Sommelier",                         3, 2000, List.of("WEITERBILDUNG_WEINKENNER_2")));
        sideCerts.put("WEITERBILDUNG_KUNSTKENNER_1",        new SideCertDef("WEITERBILDUNG_KUNSTKENNER_1",        "Kunstgeschichte Einführung",               1, 400,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_KUNSTKENNER_2",        new SideCertDef("WEITERBILDUNG_KUNSTKENNER_2",        "Kunstmarkt-Experte",                       2, 900,  List.of("WEITERBILDUNG_KUNSTKENNER_1")));
        sideCerts.put("WEITERBILDUNG_KUNSTKENNER_3",        new SideCertDef("WEITERBILDUNG_KUNSTKENNER_3",        "Art Advisor Zertifikat",                   3, 2000, List.of("WEITERBILDUNG_KUNSTKENNER_2")));
        sideCerts.put("WEITERBILDUNG_UHRMACHER_1",          new SideCertDef("WEITERBILDUNG_UHRMACHER_1",          "Uhrmacher-Grundkurs",                      1, 500,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_UHRMACHER_2",          new SideCertDef("WEITERBILDUNG_UHRMACHER_2",          "Zertifizierter Uhrenexperte",              2, 1000, List.of("WEITERBILDUNG_UHRMACHER_1")));
        sideCerts.put("WEITERBILDUNG_UHRMACHER_3",          new SideCertDef("WEITERBILDUNG_UHRMACHER_3",          "Horologie Diplom",                         3, 2500, List.of("WEITERBILDUNG_UHRMACHER_2")));
        sideCerts.put("WEITERBILDUNG_NUMISMATIK_1",         new SideCertDef("WEITERBILDUNG_NUMISMATIK_1",         "Münzkunde Grundkurs",                      1, 250,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_NUMISMATIK_2",         new SideCertDef("WEITERBILDUNG_NUMISMATIK_2",         "Professioneller Numismatiker",             2, 600,  List.of("WEITERBILDUNG_NUMISMATIK_1")));
        sideCerts.put("WEITERBILDUNG_PHILATELIE_1",         new SideCertDef("WEITERBILDUNG_PHILATELIE_1",         "Briefmarken-Sammler Kurs",                 1, 200,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_PHILATELIE_2",         new SideCertDef("WEITERBILDUNG_PHILATELIE_2",         "Philatelie-Experte",                       2, 500,  List.of("WEITERBILDUNG_PHILATELIE_1")));
        sideCerts.put("WEITERBILDUNG_MINERALIEN_1",         new SideCertDef("WEITERBILDUNG_MINERALIEN_1",         "Gemmologie Grundkurs",                     1, 450,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_MINERALIEN_2",         new SideCertDef("WEITERBILDUNG_MINERALIEN_2",         "Zertifizierter Gemmologe (FGA)",           2, 1000, List.of("WEITERBILDUNG_MINERALIEN_1")));
        sideCerts.put("WEITERBILDUNG_MINERALIEN_3",         new SideCertDef("WEITERBILDUNG_MINERALIEN_3",         "Diamond Grading Expert",                   2, 2200, List.of("WEITERBILDUNG_MINERALIEN_2")));
        sideCerts.put("WEITERBILDUNG_SPORTSAMMLER_1",       new SideCertDef("WEITERBILDUNG_SPORTSAMMLER_1",       "Sport-Memorabilia Grundkurs",              1, 200,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_SPORTSAMMLER_2",       new SideCertDef("WEITERBILDUNG_SPORTSAMMLER_2",       "Sportartefakt-Authentifizierer",           2, 500,  List.of("WEITERBILDUNG_SPORTSAMMLER_1")));
        sideCerts.put("WEITERBILDUNG_WHISKY_1",             new SideCertDef("WEITERBILDUNG_WHISKY_1",             "Whisky & Spirituosen Grundkurs",           1, 350,  List.of("REALSCHULABSCHLUSS")));
        sideCerts.put("WEITERBILDUNG_WHISKY_2",             new SideCertDef("WEITERBILDUNG_WHISKY_2",             "Master Distiller Zertifikat",              2, 900,  List.of("WEITERBILDUNG_WHISKY_1")));
        log.info("EducationService: Standarddaten geladen");
    }

    // -------------------------------------------------------------------------
    // Public label helpers (used by JobService for DTO display names)
    // -------------------------------------------------------------------------

    public static String stageLabel(String stageKey) {
        if (stageKey == null) return "";
        return switch (stageKey) {
            case "GRUNDSCHULE"        -> "Grundschule";
            case "REALSCHULABSCHLUSS" -> "Realschulabschluss";
            case "ABITUR"             -> "Abitur";
            default -> {
                if (stageKey.startsWith("AUSBILDUNG_")) yield "Ausbildung: " + fieldLabel(stageKey.substring(11));
                if (stageKey.startsWith("BACHELOR_"))  yield "Bachelor: "   + fieldLabel(stageKey.substring(9));
                if (stageKey.startsWith("MASTER_"))    yield "Master: "     + fieldLabel(stageKey.substring(7));
                yield stageKey;
            }
        };
    }

    public static String sideCertLabel(String key) {
        if (key == null) return null;
        return switch (key) {
            // Legacy single-level keys (backwards compat)
            case "WEITERBILDUNG_BARKEEPER"          -> "Barkeeper-Kurs";
            case "WEITERBILDUNG_FITNESSTRAINER"     -> "Fitnesstrainer B-Lizenz";
            case "WEITERBILDUNG_SOCIAL_MEDIA"       -> "Social-Media-Marketing";
            case "WEITERBILDUNG_EXCEL"              -> "Excel & Datenanalyse";
            case "WEITERBILDUNG_FUEHRERSCHEIN"      -> "Führerschein Klasse B";
            case "WEITERBILDUNG_CRYPTO"             -> "Krypto-Trading Zertifikat";
            case "WEITERBILDUNG_BUCHHALTUNG"        -> "Buchhaltung & DATEV";
            case "WEITERBILDUNG_IMMOBILIEN"         -> "Immobilienmakler-Lizenz";
            case "WEITERBILDUNG_PROJEKTMANAGEMENT"  -> "Projektmanagement (PRINCE2)";
            case "WEITERBILDUNG_STEUERN"            -> "Steuerberater-Grundkurs";
            case "WEITERBILDUNG_HACKER"             -> "Ethical Hacking Zertifikat";
            // Multi-level cert keys
            case "WEITERBILDUNG_BARKEEPER_1"         -> "Barkeeper-Kurs (Grundlagen)";
            case "WEITERBILDUNG_BARKEEPER_2"         -> "Barkeeper-Kurs (Fortgeschritten)";
            case "WEITERBILDUNG_BARKEEPER_3"         -> "Bar-Manager Zertifikat";
            case "WEITERBILDUNG_FITNESSTRAINER_1"    -> "Fitnesstrainer B-Lizenz";
            case "WEITERBILDUNG_FITNESSTRAINER_2"    -> "Fitnesstrainer A-Lizenz";
            case "WEITERBILDUNG_FITNESSTRAINER_3"    -> "Personal Trainer Zertifikat";
            case "WEITERBILDUNG_SOCIAL_MEDIA_1"      -> "Social-Media-Marketing (Grundkurs)";
            case "WEITERBILDUNG_SOCIAL_MEDIA_2"      -> "Social-Media-Marketing (Aufbaukurs)";
            case "WEITERBILDUNG_SOCIAL_MEDIA_3"      -> "Social-Media-Marketing (Expertenzertifikat)";
            case "WEITERBILDUNG_EXCEL_1"             -> "Excel & Datenanalyse (Grundkurs)";
            case "WEITERBILDUNG_EXCEL_2"             -> "Excel & Power BI (Aufbaukurs)";
            case "WEITERBILDUNG_EXCEL_3"             -> "Data Analyst Zertifikat (Microsoft)";
            case "WEITERBILDUNG_FUEHRERSCHEIN_1"     -> "Führerschein Klasse B";
            case "WEITERBILDUNG_FUEHRERSCHEIN_2"     -> "Führerschein Klasse BE + Anhänger";
            case "WEITERBILDUNG_FUEHRERSCHEIN_3"     -> "LKW-Führerschein Klasse C+E";
            case "WEITERBILDUNG_CRYPTO_1"            -> "Krypto-Trading Zertifikat (Grundlagen)";
            case "WEITERBILDUNG_CRYPTO_2"            -> "DeFi & Blockchain Zertifikat";
            case "WEITERBILDUNG_CRYPTO_3"            -> "Certified Crypto Analyst (CCA)";
            case "WEITERBILDUNG_BUCHHALTUNG_1"       -> "Buchhaltung & DATEV (Grundkurs)";
            case "WEITERBILDUNG_BUCHHALTUNG_2"       -> "Bilanzbuchhaltung (IHK)";
            case "WEITERBILDUNG_BUCHHALTUNG_3"       -> "Bilanzbuchhalter Zertifikat";
            case "WEITERBILDUNG_IMMOBILIEN_1"        -> "Immobilien-Grundlagen (IHK)";
            case "WEITERBILDUNG_IMMOBILIEN_2"        -> "Immobilienmakler-Lizenz";
            case "WEITERBILDUNG_IMMOBILIEN_3"        -> "Immobilien-Investor Masterclass";
            case "WEITERBILDUNG_PROJEKTMANAGEMENT_1" -> "Projektmanagement Grundlagen (PMI)";
            case "WEITERBILDUNG_PROJEKTMANAGEMENT_2" -> "Projektmanagement (PRINCE2 Foundation)";
            case "WEITERBILDUNG_PROJEKTMANAGEMENT_3" -> "PMP Zertifizierung";
            case "WEITERBILDUNG_STEUERN_1"           -> "Steuerlehre Grundkurs";
            case "WEITERBILDUNG_STEUERN_2"           -> "Steuerberater-Vorbereitung";
            case "WEITERBILDUNG_STEUERN_3"           -> "Steuerberater-Examen";
            case "WEITERBILDUNG_HACKER_1"            -> "IT-Security Grundlagen";
            case "WEITERBILDUNG_HACKER_2"            -> "Ethical Hacking Zertifikat (CEH)";
            case "WEITERBILDUNG_HACKER_3"            -> "OSCP Penetration Testing";
            case "WEITERBILDUNG_IMMOBILIEN_4"        -> "Immobilien-Portfolio Manager";
            case "WEITERBILDUNG_OLDTIMER_1"          -> "Oldtimer-Kurs Grundlagen";
            case "WEITERBILDUNG_OLDTIMER_2"          -> "Classic-Car Experte";
            case "WEITERBILDUNG_OLDTIMER_3"          -> "Oldtimer-Auktionator Zertifikat";
            case "WEITERBILDUNG_ARCHAEOLOGIE_1"      -> "Archäologen-Hobbykurs";
            case "WEITERBILDUNG_ARCHAEOLOGIE_2"      -> "Antiquitäten-Experte";
            case "WEITERBILDUNG_WEINKENNER_1"        -> "Weinkenner Grundkurs";
            case "WEITERBILDUNG_WEINKENNER_2"        -> "Wine & Spirit Education (WSET)";
            case "WEITERBILDUNG_WEINKENNER_3"        -> "Master Sommelier";
            case "WEITERBILDUNG_KUNSTKENNER_1"       -> "Kunstgeschichte Einführung";
            case "WEITERBILDUNG_KUNSTKENNER_2"       -> "Kunstmarkt-Experte";
            case "WEITERBILDUNG_KUNSTKENNER_3"       -> "Art Advisor Zertifikat";
            case "WEITERBILDUNG_UHRMACHER_1"         -> "Uhrmacher-Grundkurs";
            case "WEITERBILDUNG_UHRMACHER_2"         -> "Zertifizierter Uhrenexperte";
            case "WEITERBILDUNG_UHRMACHER_3"         -> "Horologie Diplom";
            case "WEITERBILDUNG_NUMISMATIK_1"        -> "Münzkunde Grundkurs";
            case "WEITERBILDUNG_NUMISMATIK_2"        -> "Professioneller Numismatiker";
            case "WEITERBILDUNG_PHILATELIE_1"        -> "Briefmarken-Sammler Kurs";
            case "WEITERBILDUNG_PHILATELIE_2"        -> "Philatelie-Experte";
            case "WEITERBILDUNG_MINERALIEN_1"        -> "Gemmologie Grundkurs";
            case "WEITERBILDUNG_MINERALIEN_2"        -> "Zertifizierter Gemmologe (FGA)";
            case "WEITERBILDUNG_MINERALIEN_3"        -> "Diamond Grading Expert";
            case "WEITERBILDUNG_SPORTSAMMLER_1"      -> "Sport-Memorabilia Grundkurs";
            case "WEITERBILDUNG_SPORTSAMMLER_2"      -> "Sportartefakt-Authentifizierer";
            case "WEITERBILDUNG_WHISKY_1"            -> "Whisky & Spirituosen Grundkurs";
            case "WEITERBILDUNG_WHISKY_2"            -> "Master Distiller Zertifikat";
            default -> key;
        };
    }

    private static String fieldLabel(String field) {
        return switch (field) {
            case "INFORMATIK"       -> "Informatik";
            case "BWL"              -> "Betriebswirtschaft";
            case "MEDIZIN"          -> "Medizin";
            case "JURA"             -> "Rechtswissenschaften";
            case "INGENIEURWESEN"   -> "Ingenieurwesen";
            case "PSYCHOLOGIE"      -> "Psychologie";
            case "FACHINFORMATIKER" -> "Fachinformatiker/-in";
            case "EINZELHANDEL"     -> "Einzelhandelskaufmann/-frau";
            case "KFZTECH"          -> "KFZ-Mechatronik";
            case "PFLEGE"           -> "Pflegefachkraft";
            case "KOCH"             -> "Koch/Köchin";
            case "ELEKTRIKER"       -> "Elektriker/-in";
            case "SOCIAL_MEDIA"     -> "Social-Media-Marketing";
            case "EXCEL"            -> "Excel & Datenanalyse";
            case "FUEHRERSCHEIN"    -> "Führerschein Klasse B";
            case "CRYPTO"           -> "Krypto-Trading";
            case "BUCHHALTUNG"      -> "Buchhaltung & DATEV";
            case "IMMOBILIEN"       -> "Immobilienmakler-Lizenz";
            case "PROJEKTMANAGEMENT"-> "Projektmanagement (PRINCE2)";
            case "STEUERN"          -> "Steuerberater-Grundkurs";
            case "HACKER"           -> "Ethical Hacking";
            case "BARKEEPER"        -> "Barkeeper-Kurs";
            case "FITNESSTRAINER"   -> "Fitnesstrainer B-Lizenz";
            default -> field;
        };
    }

    // -------------------------------------------------------------------------
    // Service methods
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public EducationProgressDto getProgress(Long playerId) {
        EducationProgress ep = findOrThrow(playerId);
        List<String> completed = Arrays.asList(ep.getCompletedStages());
        return EducationProgressDto.from(ep,
            buildAvailableMainStages(ep, completed),
            buildAvailableSideCerts(ep, completed));
    }

    @Transactional
    public EducationProgressDto enrollMain(Long playerId, EnrollMainRequest req) {
        EducationProgress ep = findOrThrow(playerId);
        List<String> completed = Arrays.asList(ep.getCompletedStages());

        if (ep.getMainStageMonthsRemaining() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Bereits in einer Ausbildung eingeschrieben");
        }

        StageDefinition def = mainStages.get(req.stage());
        if (def == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Stufe");
        }

        if (def.requiresField() && (req.field() == null || req.field().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Fachrichtung erforderlich fuer diese Stufe");
        }

        if (def.requiresField()) {
            boolean validField = def.fieldOptions().stream()
                .anyMatch(fo -> fo.value().equals(req.field()));
            if (!validField) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungueltige Fachrichtung");
            }
        }

        // Prerequisite check
        if ("BACHELOR".equals(def.parentKey())) {
            // MASTER → requires matching BACHELOR field
            String bachelorKey = "BACHELOR_" + req.field();
            if (!completed.contains(bachelorKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Voraussetzung nicht erfuellt: Bachelor in " + fieldLabel(req.field()));
            }
        } else {
            String parentKey = resolveKey(def.parentKey(), null);
            if (!completed.contains(parentKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Voraussetzung nicht erfuellt: " + stageLabel(parentKey));
            }
        }

        String targetKey = resolveKey(req.stage(), req.field());
        if (completed.contains(targetKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stufe bereits abgeschlossen");
        }

        // Deduct cost
        if (def.cost() > 0) {
            characterService.deductCash(playerId, BigDecimal.valueOf(def.cost()),
                stageLabel(targetKey));
        }

        // Use per-field duration override if available, else stage default
        int duration = def.fieldDurations().getOrDefault(req.field(), def.durationMonths());

        ep.setMainStage(req.stage());
        ep.setMainStageField(req.field());
        ep.setMainStageMonthsRemaining(duration);
        educationProgressRepository.save(ep);

        return getProgress(playerId);
    }

    @Transactional
    public EducationProgressDto enrollSide(Long playerId, EnrollSideRequest req) {
        EducationProgress ep = findOrThrow(playerId);
        List<String> completed = Arrays.asList(ep.getCompletedStages());

        if (ep.getSideCertMonthsRemaining() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Bereits in einer Weiterbildung eingeschrieben");
        }

        SideCertDef cert = sideCerts.get(req.cert());
        if (cert == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Weiterbildung");
        }

        // Per-cert requirement check (OR-logic)
        if (!cert.requiresAny().isEmpty()) {
            boolean meetsReq = cert.requiresAny().stream().anyMatch(completed::contains);
            if (!meetsReq) {
                String needed = cert.requiresAny().size() == 1
                    ? stageLabel(cert.requiresAny().get(0))
                    : "eines der folgenden: " + String.join(", ",
                        cert.requiresAny().stream().map(EducationService::stageLabel).toList());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Voraussetzung nicht erfuellt: " + needed);
            }
        }

        // certKey is now the full stored key (e.g. "WEITERBILDUNG_SOCIAL_MEDIA_2")
        String certKey = cert.certKey();
        if (completed.contains(certKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Weiterbildung bereits abgeschlossen");
        }

        // Deduct cost
        if (cert.cost() > 0) {
            characterService.deductCash(playerId, BigDecimal.valueOf(cert.cost()), cert.label());
        }

        ep.setSideCert(certKey);
        ep.setSideCertMonthsRemaining(cert.durationMonths());
        educationProgressRepository.save(ep);

        return getProgress(playerId);
    }

    // -------------------------------------------------------------------------

    private List<AvailableStageDto> buildAvailableMainStages(EducationProgress ep,
                                                              List<String> completed) {
        if (ep.getMainStageMonthsRemaining() > 0) return List.of();

        List<AvailableStageDto> result = new ArrayList<>();
        for (Map.Entry<String, StageDefinition> entry : mainStages.entrySet()) {
            String stageType = entry.getKey();
            StageDefinition def = entry.getValue();

            if ("MASTER".equals(stageType)) {
                // Only show MASTER fields where matching BACHELOR exists and MASTER not yet done
                List<FieldOption> availableFields = def.fieldOptions().stream()
                    .filter(fo -> completed.contains("BACHELOR_" + fo.value())
                                  && !completed.contains("MASTER_" + fo.value()))
                    .toList();
                if (availableFields.isEmpty()) continue;
                result.add(new AvailableStageDto(stageType, stageLabel(stageType),
                    def.durationMonths(), true, availableFields, def.cost()));
            } else {
                // Regular stages: check parent key
                String parentKey = resolveKey(def.parentKey(), null);
                if (!completed.contains(parentKey)) continue;

                if (!def.requiresField()) {
                    if (completed.contains(stageType)) continue;
                    result.add(new AvailableStageDto(stageType, stageLabel(stageType),
                        def.durationMonths(), false, List.of(), def.cost()));
                } else {
                    List<FieldOption> remainingFields = def.fieldOptions().stream()
                        .filter(fo -> !completed.contains(stageType + "_" + fo.value()))
                        .toList();
                    if (remainingFields.isEmpty()) continue;
                    result.add(new AvailableStageDto(stageType, stageLabel(stageType),
                        def.durationMonths(), true, remainingFields, def.cost()));
                }
            }
        }
        return result;
    }

    private List<SideCertDto> buildAvailableSideCerts(EducationProgress ep,
                                                       List<String> completed) {
        if (ep.getSideCertMonthsRemaining() > 0) return List.of();

        return sideCerts.entrySet().stream()
            .filter(e -> {
                SideCertDef def = e.getValue();
                // Not already completed (certKey is the full stored key)
                if (completed.contains(def.certKey())) return false;
                // Check per-cert requirements (OR-logic)
                List<String> reqs = def.requiresAny();
                return reqs.isEmpty() || reqs.stream().anyMatch(completed::contains);
            })
            .map(e -> {
                SideCertDef def = e.getValue();
                return new SideCertDto(e.getKey(), def.label(), def.durationMonths(), def.cost());
            })
            .toList();
    }

    private static String resolveKey(String stage, String field) {
        if (field != null && !field.isBlank()) return stage + "_" + field;
        return stage;
    }

    private EducationProgress findOrThrow(Long playerId) {
        return educationProgressRepository.findByPlayerId(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Bildungsfortschritt nicht gefunden"));
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseRequiresAny(Map<String, Object> map) {
        List<String> result = new ArrayList<>();
        Object singleReq = map.get("requires");
        if (singleReq instanceof String sr && !sr.isBlank()) result.add(sr);
        Object listReq = map.get("requiresAny");
        if (listReq instanceof List<?> lr) {
            for (Object r : lr) {
                if (r instanceof String rs && !rs.isBlank()) result.add(rs);
            }
        }
        return result;
    }

    private static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        return Integer.parseInt(val.toString());
    }
}
