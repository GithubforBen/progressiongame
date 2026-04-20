package com.financegame.service;

import com.financegame.domain.condition.*;
import com.financegame.domain.social.BoostDef;
import com.financegame.domain.social.GiftRequirement;
import com.financegame.domain.social.GroupDef;
import com.financegame.domain.social.PersonDef;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
public class PersonService {

    private static final Logger log = LoggerFactory.getLogger(PersonService.class);

    private final Map<String, PersonDef> personsById = new LinkedHashMap<>();
    private final Map<String, GroupDef> groupsById = new LinkedHashMap<>();

    @PostConstruct
    public void loadPersonsData() {
        try (InputStream is = getClass().getResourceAsStream("/data/persons.yaml")) {
            if (is == null) {
                log.warn("persons.yaml not found, loading defaults");
                loadDefaults();
                return;
            }
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            parseGroups(root);
            parsePersons(root);
            log.info("Loaded {} groups and {} persons from persons.yaml",
                groupsById.size(), personsById.size());
        } catch (Exception e) {
            log.error("Failed to load persons.yaml, loading defaults", e);
            loadDefaults();
        }
    }

    @SuppressWarnings("unchecked")
    private void parseGroups(Map<String, Object> root) {
        List<Map<String, Object>> groups = (List<Map<String, Object>>) root.get("groups");
        if (groups == null) return;
        for (Map<String, Object> g : groups) {
            String id = (String) g.get("id");
            String name = (String) g.get("name");
            String type = (String) g.get("type");
            String description = (String) g.getOrDefault("description", "");
            Condition exclusiveCond = parseCondition(g.get("exclusiveCondition"));
            groupsById.put(id, new GroupDef(id, name, type, description, exclusiveCond));
        }
    }

    @SuppressWarnings("unchecked")
    private void parsePersons(Map<String, Object> root) {
        List<Map<String, Object>> persons = (List<Map<String, Object>>) root.get("persons");
        if (persons == null) return;
        for (Map<String, Object> p : persons) {
            String id = (String) p.get("id");
            String name = (String) p.get("name");
            String description = (String) p.getOrDefault("description", "");
            List<String> groupIds = (List<String>) p.getOrDefault("groupIds", List.of());
            int networkThreshold = ((Number) p.getOrDefault("networkThreshold", 50)).intValue();
            List<String> friendNetwork = (List<String>) p.getOrDefault("friendNetwork", List.of());
            int wealthLevel = ((Number) p.getOrDefault("wealthLevel", 2)).intValue();

            BoostDef boost = parseBoost(p.get("boost"));
            Condition unlockCondition = parseCondition(p.get("unlockCondition"));
            GiftRequirement giftRequirement = parseGiftRequirement(p.get("giftRequirement"));

            personsById.put(id, new PersonDef(id, name, description, groupIds, networkThreshold,
                friendNetwork, boost, unlockCondition, giftRequirement, wealthLevel));
        }
    }

    @SuppressWarnings("unchecked")
    private BoostDef parseBoost(Object raw) {
        if (raw == null) return new BoostDef("NONE", 0);
        Map<String, Object> m = (Map<String, Object>) raw;
        String type = (String) m.get("type");
        double value = ((Number) m.getOrDefault("value", 0)).doubleValue();
        return new BoostDef(type, value);
    }

    @SuppressWarnings("unchecked")
    private GiftRequirement parseGiftRequirement(Object raw) {
        if (raw == null) return new GiftRequirement(0, null, "");
        Map<String, Object> m = (Map<String, Object>) raw;
        int cost = ((Number) m.getOrDefault("cost", 0)).intValue();
        String description = (String) m.getOrDefault("description", "");
        Condition condition = parseCondition(m.get("condition"));
        return new GiftRequirement(cost, condition, description);
    }

    @SuppressWarnings("unchecked")
    Condition parseCondition(Object raw) {
        if (raw == null) return null;
        Map<String, Object> m = (Map<String, Object>) raw;
        String type = (String) m.get("type");
        if (type == null) return null;

        return switch (type) {
            case "education-level" -> new EducationLevelCondition((String) m.get("prefix"));
            case "min-net-worth" -> new MinNetWorthCondition(
                BigDecimal.valueOf(((Number) m.get("amount")).doubleValue()));
            case "has-relationship-score" -> new HasRelationshipScoreCondition(
                (String) m.get("personId"),
                ((Number) m.get("minScore")).intValue());
            case "had-conflict-with" -> new HadConflictWithCondition((String) m.get("personId"));
            case "min-jail-months-served" -> new MinJailMonthsServedCondition(
                ((Number) m.get("months")).intValue());
            case "owns-collection" -> new OwnsCollectionCondition((String) m.get("collectionName"));
            case "has-cert" -> new HasCertCondition((String) m.get("certKey"));
            case "all-of" -> {
                List<Object> conds = (List<Object>) m.get("conditions");
                Condition[] parsed = conds.stream()
                    .map(this::parseCondition)
                    .filter(Objects::nonNull)
                    .toArray(Condition[]::new);
                yield new AllOfCondition(parsed);
            }
            case "any-of" -> {
                List<Object> conds = (List<Object>) m.get("conditions");
                Condition[] parsed = conds.stream()
                    .map(this::parseCondition)
                    .filter(Objects::nonNull)
                    .toArray(Condition[]::new);
                yield new AnyOfCondition(parsed);
            }
            default -> {
                log.warn("Unknown condition type: {}", type);
                yield null;
            }
        };
    }

    private void loadDefaults() {
        groupsById.put("NACHBARSCHAFT", new GroupDef("NACHBARSCHAFT", "Nachbarschaft", "OPEN", "Deine direkten Nachbarn", null));
        personsById.put("HANS_MUELLER", new PersonDef("HANS_MUELLER", "Hans Müller",
            "Freundlicher Rentner", List.of("NACHBARSCHAFT"), 40, List.of(),
            new BoostDef("HAPPINESS_PER_TURN", 3), null,
            new GiftRequirement(30, null, "Kleines Mitbringsel"), 1));
    }

    public Collection<PersonDef> getAllPersons() {
        return personsById.values();
    }

    public Optional<PersonDef> findById(String personId) {
        return Optional.ofNullable(personsById.get(personId));
    }

    public Collection<GroupDef> getAllGroups() {
        return groupsById.values();
    }

    public Optional<GroupDef> findGroupById(String groupId) {
        return Optional.ofNullable(groupsById.get(groupId));
    }

    public List<PersonDef> getPersonsInGroup(String groupId) {
        return personsById.values().stream()
            .filter(p -> p.groupIds().contains(groupId))
            .toList();
    }
}
