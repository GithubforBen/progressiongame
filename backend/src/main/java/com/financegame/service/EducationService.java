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
import java.util.*;

@Service
public class EducationService {

    private static final Logger log = LoggerFactory.getLogger(EducationService.class);

    record StageDefinition(String parentKey, int durationMonths,
                           boolean requiresField, List<FieldOption> fieldOptions) {}

    // Populated from YAML on startup; fallback to static data if loading fails
    private final Map<String, StageDefinition> mainStages = new LinkedHashMap<>();
    private final Map<String, SideCertDto> sideCerts = new LinkedHashMap<>();

    private final EducationProgressRepository educationProgressRepository;

    public EducationService(EducationProgressRepository educationProgressRepository) {
        this.educationProgressRepository = educationProgressRepository;
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
                    List<FieldOption> fields = new ArrayList<>();
                    List<Map<String, Object>> fieldList = (List<Map<String, Object>>) s.get("fields");
                    if (fieldList != null) {
                        for (Map<String, Object> f : fieldList) {
                            fields.add(new FieldOption((String) f.get("value"), (String) f.get("label")));
                        }
                    }
                    mainStages.put(key, new StageDefinition(parentKey, duration, requiresField, fields));
                }
            }

            List<Map<String, Object>> certs = (List<Map<String, Object>>) data.get("sideCerts");
            if (certs != null) {
                for (Map<String, Object> c : certs) {
                    String key = (String) c.get("key");
                    String label = (String) c.get("label");
                    int duration = toInt(c.get("durationMonths"));
                    sideCerts.put(key, new SideCertDto(key, label, duration));
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
        var noFields = List.<FieldOption>of();
        mainStages.put("REALSCHULABSCHLUSS", new StageDefinition("GRUNDSCHULE",     2, false, noFields));
        mainStages.put("ABITUR",             new StageDefinition("REALSCHULABSCHLUSS", 3, false, noFields));
        mainStages.put("AUSBILDUNG", new StageDefinition("REALSCHULABSCHLUSS", 4, true, List.of(
            new FieldOption("FACHINFORMATIKER", "Fachinformatiker"),
            new FieldOption("KAUFMANN",         "Kaufmann/-frau"),
            new FieldOption("ELEKTRIKER",       "Elektriker/-in"),
            new FieldOption("ERZIEHER",         "Erzieher/-in")
        )));
        mainStages.put("BACHELOR", new StageDefinition("ABITUR", 6, true, List.of(
            new FieldOption("INFORMATIK", "Informatik"),
            new FieldOption("BWL",        "Betriebswirtschaft"),
            new FieldOption("MEDIZIN",    "Medizin"),
            new FieldOption("JURA",       "Rechtswissenschaften")
        )));
        mainStages.put("MASTER", new StageDefinition("BACHELOR", 4, true, List.of(
            new FieldOption("INFORMATIK", "Informatik"),
            new FieldOption("BWL",        "Betriebswirtschaft"),
            new FieldOption("MEDIZIN",    "Medizin"),
            new FieldOption("JURA",       "Rechtswissenschaften")
        )));
        sideCerts.put("SOCIAL_MEDIA",   new SideCertDto("SOCIAL_MEDIA",   "Social Media Marketing",    1));
        sideCerts.put("EXCEL",          new SideCertDto("EXCEL",          "Excel-Kurs",                1));
        sideCerts.put("FUEHRERSCHEIN",  new SideCertDto("FUEHRERSCHEIN",  "Fuehrerschein",             1));
        sideCerts.put("CRYPTO",         new SideCertDto("CRYPTO",         "Crypto Trading Zertifikat", 1));
        log.info("EducationService: Standarddaten geladen");
    }

    // Display labels for stage keys
    public static String stageLabel(String stageKey) {
        if (stageKey == null) return "";
        return switch (stageKey) {
            case "GRUNDSCHULE"       -> "Grundschule";
            case "REALSCHULABSCHLUSS"-> "Realschulabschluss";
            case "ABITUR"            -> "Abitur";
            default -> {
                if (stageKey.startsWith("AUSBILDUNG_")) yield "Ausbildung: " + fieldLabel(stageKey.substring(11));
                if (stageKey.startsWith("BACHELOR_"))  yield "Bachelor: "   + fieldLabel(stageKey.substring(9));
                if (stageKey.startsWith("MASTER_"))    yield "Master: "     + fieldLabel(stageKey.substring(7));
                yield stageKey;
            }
        };
    }

    private static String fieldLabel(String field) {
        return switch (field) {
            case "INFORMATIK"       -> "Informatik";
            case "BWL"              -> "Betriebswirtschaft";
            case "MEDIZIN"          -> "Medizin";
            case "JURA"             -> "Rechtswissenschaften";
            case "FACHINFORMATIKER" -> "Fachinformatiker";
            case "KAUFMANN"         -> "Kaufmann/-frau";
            case "ELEKTRIKER"       -> "Elektriker/-in";
            case "ERZIEHER"         -> "Erzieher/-in";
            case "SOCIAL_MEDIA"     -> "Social Media Marketing";
            case "EXCEL"            -> "Excel-Kurs";
            case "FUEHRERSCHEIN"    -> "Fuehrerschein";
            case "CRYPTO"           -> "Crypto Trading";
            default -> field;
        };
    }

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

        String parentKey = resolveKey(def.parentKey(), null);
        if ("BACHELOR".equals(def.parentKey())) {
            String bachelorKey = "BACHELOR_" + req.field();
            if (!completed.contains(bachelorKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Voraussetzung nicht erfuellt: Bachelor in " + fieldLabel(req.field()));
            }
        } else if (!completed.contains(parentKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Voraussetzung nicht erfuellt: " + stageLabel(parentKey));
        }

        String targetKey = resolveKey(req.stage(), req.field());
        if (completed.contains(targetKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stufe bereits abgeschlossen");
        }

        ep.setMainStage(req.stage());
        ep.setMainStageField(req.field());
        ep.setMainStageMonthsRemaining(def.durationMonths());
        educationProgressRepository.save(ep);

        return getProgress(playerId);
    }

    @Transactional
    public EducationProgressDto enrollSide(Long playerId, EnrollSideRequest req) {
        EducationProgress ep = findOrThrow(playerId);
        List<String> completed = Arrays.asList(ep.getCompletedStages());

        if (!completed.contains("REALSCHULABSCHLUSS")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Weiterbildungen erfordern mindestens Realschulabschluss");
        }
        if (ep.getSideCertMonthsRemaining() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Bereits in einer Weiterbildung eingeschrieben");
        }

        SideCertDto cert = sideCerts.get(req.cert());
        if (cert == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Weiterbildung");
        }
        if (completed.contains("WEITERBILDUNG_" + req.cert())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Weiterbildung bereits abgeschlossen");
        }

        ep.setSideCert("WEITERBILDUNG_" + req.cert());
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
                boolean hasBachelor = completed.stream().anyMatch(s -> s.startsWith("BACHELOR_"));
                if (!hasBachelor) continue;
            } else {
                String parentKey = resolveKey(def.parentKey(), null);
                if (!completed.contains(parentKey)) continue;
            }

            if (!def.requiresField()) {
                if (completed.contains(stageType)) continue;
                result.add(new AvailableStageDto(stageType, stageLabel(stageType),
                    def.durationMonths(), false, List.of()));
            } else {
                List<FieldOption> remainingFields = def.fieldOptions().stream()
                    .filter(fo -> !completed.contains(stageType + "_" + fo.value()))
                    .toList();
                if (remainingFields.isEmpty()) continue;
                result.add(new AvailableStageDto(stageType, stageLabel(stageType),
                    def.durationMonths(), true, remainingFields));
            }
        }
        return result;
    }

    private List<SideCertDto> buildAvailableSideCerts(EducationProgress ep,
                                                       List<String> completed) {
        if (!completed.contains("REALSCHULABSCHLUSS")) return List.of();
        if (ep.getSideCertMonthsRemaining() > 0) return List.of();

        return sideCerts.entrySet().stream()
            .filter(e -> !completed.contains("WEITERBILDUNG_" + e.getKey()))
            .map(Map.Entry::getValue)
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

    private static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        return Integer.parseInt(val.toString());
    }
}
