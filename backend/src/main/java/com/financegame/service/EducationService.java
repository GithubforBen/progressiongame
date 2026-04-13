package com.financegame.service;

import com.financegame.dto.EducationProgressDto;
import com.financegame.dto.EducationProgressDto.*;
import com.financegame.dto.EnrollMainRequest;
import com.financegame.dto.EnrollSideRequest;
import com.financegame.entity.EducationProgress;
import com.financegame.repository.EducationProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class EducationService {

    // -------------------------------------------------------------------------
    // Static stage catalogue
    // -------------------------------------------------------------------------

    record StageDefinition(String parentKey, int durationMonths,
                           boolean requiresField, List<FieldOption> fieldOptions) {}

    private static final Map<String, StageDefinition> MAIN_STAGES = new LinkedHashMap<>();
    private static final Map<String, SideCertDto> SIDE_CERTS = new LinkedHashMap<>();

    static {
        var noFields = List.<FieldOption>of();

        MAIN_STAGES.put("REALSCHULABSCHLUSS", new StageDefinition("GRUNDSCHULE",     2, false, noFields));
        MAIN_STAGES.put("ABITUR",             new StageDefinition("REALSCHULABSCHLUSS", 3, false, noFields));
        MAIN_STAGES.put("AUSBILDUNG", new StageDefinition("REALSCHULABSCHLUSS", 4, true, List.of(
            new FieldOption("FACHINFORMATIKER", "Fachinformatiker"),
            new FieldOption("KAUFMANN",         "Kaufmann/-frau"),
            new FieldOption("ELEKTRIKER",       "Elektriker/-in"),
            new FieldOption("ERZIEHER",         "Erzieher/-in")
        )));
        MAIN_STAGES.put("BACHELOR", new StageDefinition("ABITUR", 6, true, List.of(
            new FieldOption("INFORMATIK", "Informatik"),
            new FieldOption("BWL",        "Betriebswirtschaft"),
            new FieldOption("MEDIZIN",    "Medizin"),
            new FieldOption("JURA",       "Rechtswissenschaften")
        )));
        MAIN_STAGES.put("MASTER", new StageDefinition("BACHELOR", 4, true, List.of(
            new FieldOption("INFORMATIK", "Informatik"),
            new FieldOption("BWL",        "Betriebswirtschaft"),
            new FieldOption("MEDIZIN",    "Medizin"),
            new FieldOption("JURA",       "Rechtswissenschaften")
        )));

        SIDE_CERTS.put("SOCIAL_MEDIA",   new SideCertDto("SOCIAL_MEDIA",   "Social Media Marketing",   1));
        SIDE_CERTS.put("EXCEL",          new SideCertDto("EXCEL",          "Excel-Kurs",               1));
        SIDE_CERTS.put("FUEHRERSCHEIN",  new SideCertDto("FUEHRERSCHEIN",  "Führerschein",             1));
        SIDE_CERTS.put("CRYPTO",         new SideCertDto("CRYPTO",         "Crypto Trading Zertifikat",1));
    }

    // Display labels for stage keys (used for events log etc.)
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
            case "INFORMATIK"     -> "Informatik";
            case "BWL"            -> "Betriebswirtschaft";
            case "MEDIZIN"        -> "Medizin";
            case "JURA"           -> "Rechtswissenschaften";
            case "FACHINFORMATIKER" -> "Fachinformatiker";
            case "KAUFMANN"       -> "Kaufmann/-frau";
            case "ELEKTRIKER"     -> "Elektriker/-in";
            case "ERZIEHER"       -> "Erzieher/-in";
            case "SOCIAL_MEDIA"   -> "Social Media Marketing";
            case "EXCEL"          -> "Excel-Kurs";
            case "FUEHRERSCHEIN"  -> "Führerschein";
            case "CRYPTO"         -> "Crypto Trading";
            default -> field;
        };
    }

    // -------------------------------------------------------------------------

    private final EducationProgressRepository educationProgressRepository;

    public EducationService(EducationProgressRepository educationProgressRepository) {
        this.educationProgressRepository = educationProgressRepository;
    }

    @Transactional(readOnly = true)
    public EducationProgressDto getProgress(Long playerId) {
        EducationProgress ep = findOrThrow(playerId);
        List<String> completed = Arrays.asList(ep.getCompletedStages());

        List<AvailableStageDto> available = buildAvailableMainStages(ep, completed);
        List<SideCertDto> availableSide  = buildAvailableSideCerts(ep, completed);

        return EducationProgressDto.from(ep, available, availableSide);
    }

    @Transactional
    public EducationProgressDto enrollMain(Long playerId, EnrollMainRequest req) {
        EducationProgress ep = findOrThrow(playerId);
        List<String> completed = Arrays.asList(ep.getCompletedStages());

        if (ep.getMainStageMonthsRemaining() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Bereits in einer Ausbildung eingeschrieben");
        }

        StageDefinition def = MAIN_STAGES.get(req.stage());
        if (def == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Stufe");
        }

        if (def.requiresField() && (req.field() == null || req.field().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Fachrichtung erforderlich fuer diese Stufe");
        }

        // Validate field is legal for this stage
        if (def.requiresField()) {
            boolean validField = def.fieldOptions().stream()
                .anyMatch(fo -> fo.value().equals(req.field()));
            if (!validField) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungueltige Fachrichtung");
            }
        }

        // Validate parent completed
        String parentKey = resolveKey(def.parentKey(), null); // parent never has a field itself
        if ("BACHELOR".equals(def.parentKey())) {
            // For MASTER: need BACHELOR_{field} with same field
            String bachelorKey = "BACHELOR_" + req.field();
            if (!completed.contains(bachelorKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Voraussetzung nicht erfuellt: Bachelor in " + fieldLabel(req.field()));
            }
        } else if (!completed.contains(parentKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Voraussetzung nicht erfuellt: " + stageLabel(parentKey));
        }

        // Check not already completed
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

        SideCertDto cert = SIDE_CERTS.get(req.cert());
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
    // Private helpers
    // -------------------------------------------------------------------------

    private List<AvailableStageDto> buildAvailableMainStages(EducationProgress ep,
                                                              List<String> completed) {
        if (ep.getMainStageMonthsRemaining() > 0) return List.of(); // busy

        List<AvailableStageDto> result = new ArrayList<>();
        for (Map.Entry<String, StageDefinition> entry : MAIN_STAGES.entrySet()) {
            String stageType = entry.getKey();
            StageDefinition def = entry.getValue();

            if ("MASTER".equals(stageType)) {
                // Available if player has any BACHELOR_{field}
                boolean hasBachelor = completed.stream().anyMatch(s -> s.startsWith("BACHELOR_"));
                if (!hasBachelor) continue;
            } else {
                String parentKey = resolveKey(def.parentKey(), null);
                if (!completed.contains(parentKey)) continue;
            }

            // Skip if all field variants completed (or no-field stage already completed)
            if (!def.requiresField()) {
                if (completed.contains(stageType)) continue;
                result.add(new AvailableStageDto(stageType, stageLabel(stageType),
                    def.durationMonths(), false, List.of()));
            } else {
                // Only show fields not yet completed
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

        return SIDE_CERTS.entrySet().stream()
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
}
