package com.financegame.service;

import com.financegame.domain.GameContext;
import com.financegame.domain.GameContextFactory;
import com.financegame.domain.events.*;
import com.financegame.domain.social.BoostDef;
import com.financegame.domain.social.GroupDef;
import com.financegame.domain.social.PersonDef;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.PlayerSocialGroupUnlock;
import com.financegame.entity.PlayerSocialRelationship;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.PlayerSocialGroupUnlockRepository;
import com.financegame.repository.PlayerSocialRelationshipRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SocialService {

    private static final int[] WEALTH_BASE = {0, 500, 2000, 10000, 50000, 200000};
    private static final int MAX_TIME_PER_MONTH = 4;
    private static final int SCORE_PER_TIME = 8;
    private static final int SCORE_PER_GIFT = 20;
    private static final int SCORE_INSULT = -15;
    private static final int ROB_CAUGHT_VICTIM_PENALTY = -40;
    private static final int ROB_CAUGHT_GROUP_PENALTY = -15;
    private static final int ROB_CAUGHT_LOCK_MONTHS = 3;
    private static final int PASSIVE_DECAY_PER_MONTH = 1;

    private final PersonService personService;
    private final GameContextFactory gameContextFactory;
    private final PlayerSocialRelationshipRepository relationshipRepo;
    private final PlayerSocialGroupUnlockRepository groupUnlockRepo;
    private final CharacterRepository characterRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SocialService(PersonService personService,
                         GameContextFactory gameContextFactory,
                         PlayerSocialRelationshipRepository relationshipRepo,
                         PlayerSocialGroupUnlockRepository groupUnlockRepo,
                         CharacterRepository characterRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.personService = personService;
        this.gameContextFactory = gameContextFactory;
        this.relationshipRepo = relationshipRepo;
        this.groupUnlockRepo = groupUnlockRepo;
        this.characterRepository = characterRepository;
        this.eventPublisher = eventPublisher;
    }

    // ---- Network query ----

    @Transactional(readOnly = true)
    public SocialNetworkDto getNetwork(Long playerId) {
        GameContext ctx = gameContextFactory.build(playerId);
        int currentTurn = ctx.character().getCurrentTurn();

        Map<String, PlayerSocialRelationship> relMap = relationshipRepo.findByPlayerId(playerId)
            .stream().collect(Collectors.toMap(PlayerSocialRelationship::getPersonId, r -> r));

        Set<String> unlockedGroupIds = groupUnlockRepo.findUnlockedGroupIds(playerId);

        List<PersonNodeDto> nodes = buildPersonNodes(ctx, relMap, unlockedGroupIds, currentTurn);
        List<PersonEdgeDto> edges = buildEdges(nodes);
        List<GroupDto> groups = buildGroupDtos(ctx, unlockedGroupIds);
        List<ActiveBoostDto> boosts = computeActiveBoosts(relMap);

        return new SocialNetworkDto(nodes, edges, groups, boosts);
    }

    private List<PersonNodeDto> buildPersonNodes(GameContext ctx,
                                                  Map<String, PlayerSocialRelationship> relMap,
                                                  Set<String> unlockedGroupIds,
                                                  int currentTurn) {
        Set<String> visiblePersonIds = new HashSet<>();

        for (PersonDef p : personService.getAllPersons()) {
            boolean groupUnlocked = p.groupIds().stream().anyMatch(gid -> {
                GroupDef g = personService.findGroupById(gid).orElse(null);
                if (g == null) return false;
                if ("OPEN".equals(g.type())) return true;
                return g.exclusiveCondition() != null && g.exclusiveCondition().isMet(ctx);
            });
            if (!groupUnlocked) continue;

            boolean condMet = p.unlockCondition() == null || p.unlockCondition().isMet(ctx);
            if (condMet) visiblePersonIds.add(p.id());

            PlayerSocialRelationship rel = relMap.get(p.id());
            if (rel != null && rel.getScore() >= p.networkThreshold()) {
                visiblePersonIds.addAll(p.friendNetwork());
            }
        }

        List<PersonNodeDto> nodes = new ArrayList<>();
        for (PersonDef p : personService.getAllPersons()) {
            if (!visiblePersonIds.contains(p.id())) continue;

            PlayerSocialRelationship rel = relMap.get(p.id());
            boolean met = rel != null;
            boolean condMet = p.unlockCondition() == null || p.unlockCondition().isMet(ctx);
            boolean locked = met && rel.getLockedActionsUntilTurn() > currentTurn;
            int score = met ? rel.getScore() : 0;

            List<String> unlockReqs = new ArrayList<>();
            if (!condMet && p.unlockCondition() != null) {
                unlockReqs.add(p.unlockCondition().describe());
            }

            nodes.add(new PersonNodeDto(
                p.id(),
                condMet ? p.name() : "???",
                p.groupIds().isEmpty() ? null : p.groupIds().get(0),
                met,
                true,
                score,
                locked,
                locked ? rel.getLockedActionsUntilTurn() : 0,
                met && !locked && rel.getMonthlyTimeSpentCount() < MAX_TIME_PER_MONTH && condMet,
                met && !locked && !rel.isMonthlyInsultDone() && condMet,
                met && !locked && !rel.isMonthlyRobAttempted() && condMet,
                p.boost(),
                unlockReqs
            ));
        }
        return nodes;
    }

    private List<PersonEdgeDto> buildEdges(List<PersonNodeDto> nodes) {
        Set<String> visibleIds = nodes.stream().map(PersonNodeDto::personId).collect(Collectors.toSet());
        Set<String> edgeKeys = new HashSet<>();
        List<PersonEdgeDto> edges = new ArrayList<>();

        for (PersonDef p : personService.getAllPersons()) {
            if (!visibleIds.contains(p.id())) continue;
            for (String friendId : p.friendNetwork()) {
                if (!visibleIds.contains(friendId)) continue;
                String key = p.id().compareTo(friendId) < 0
                    ? p.id() + "|" + friendId
                    : friendId + "|" + p.id();
                if (edgeKeys.add(key)) {
                    edges.add(new PersonEdgeDto(p.id(), friendId));
                }
            }
        }
        return edges;
    }

    private List<GroupDto> buildGroupDtos(GameContext ctx, Set<String> unlockedGroupIds) {
        return personService.getAllGroups().stream()
            .filter(g -> {
                if ("OPEN".equals(g.type())) return true;
                return g.exclusiveCondition() != null && g.exclusiveCondition().isMet(ctx);
            })
            .map(g -> new GroupDto(g.id(), g.name(), g.type()))
            .toList();
    }

    private List<ActiveBoostDto> computeActiveBoosts(Map<String, PlayerSocialRelationship> relMap) {
        Map<String, Double> boostTotals = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerSocialRelationship> entry : relMap.entrySet()) {
            PersonDef p = personService.findById(entry.getKey()).orElse(null);
            if (p == null || p.boost() == null) continue;
            int score = entry.getValue().getScore();
            if (score <= 0) continue;
            double factor = score / 100.0;
            double effective = p.boost().value() * factor;
            boostTotals.merge(p.boost().type(), effective, Double::sum);
        }
        return boostTotals.entrySet().stream()
            .map(e -> new ActiveBoostDto(e.getKey(), Math.round(e.getValue() * 1000.0) / 1000.0))
            .toList();
    }

    // ---- Actions ----

    @Transactional
    public ActionResultDto spendTime(Long playerId, String personId) {
        GameContext ctx = gameContextFactory.build(playerId);
        PersonDef person = requirePerson(personId);
        requireConditionMet(person, ctx);

        PlayerSocialRelationship rel = getOrCreateRelationship(playerId, personId, ctx.character().getCurrentTurn());
        requireNotLocked(rel, ctx.character().getCurrentTurn());

        if (rel.getMonthlyTimeSpentCount() >= MAX_TIME_PER_MONTH) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Du hast diesen Monat bereits " + MAX_TIME_PER_MONTH + "× Zeit verbracht.");
        }

        int oldScore = rel.getScore();
        rel.setScore(Math.min(100, oldScore + SCORE_PER_TIME));
        rel.setMonthlyTimeSpentCount(rel.getMonthlyTimeSpentCount() + 1);
        relationshipRepo.save(rel);

        eventPublisher.publishEvent(new RelationshipChangedEvent(playerId, personId, oldScore, rel.getScore(), "Zeit verbracht"));
        checkNetworkThresholds(playerId, personId, oldScore, rel.getScore(), ctx.character().getCurrentTurn());

        return new ActionResultDto(rel.getScore(), "Zeit mit " + person.name() + " verbracht. +" + SCORE_PER_TIME + " Beziehungspunkte.");
    }

    @Transactional
    public ActionResultDto giveGift(Long playerId, String personId) {
        GameContext ctx = gameContextFactory.build(playerId);
        PersonDef person = requirePerson(personId);
        requireConditionMet(person, ctx);

        PlayerSocialRelationship rel = getOrCreateRelationship(playerId, personId, ctx.character().getCurrentTurn());
        requireNotLocked(rel, ctx.character().getCurrentTurn());

        if (person.giftRequirement() != null && person.giftRequirement().condition() != null
            && !person.giftRequirement().condition().isMet(ctx)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Voraussetzung nicht erfüllt: " + person.giftRequirement().condition().describe());
        }

        int cost = person.giftRequirement() != null ? person.giftRequirement().cost() : 0;
        GameCharacter character = ctx.character();
        if (character.getCash().compareTo(BigDecimal.valueOf(cost)) < 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Nicht genug Geld.");
        }

        character.setCash(character.getCash().subtract(BigDecimal.valueOf(cost)));
        characterRepository.save(character);

        int oldScore = rel.getScore();
        rel.setScore(Math.min(100, oldScore + SCORE_PER_GIFT));
        relationshipRepo.save(rel);

        eventPublisher.publishEvent(new RelationshipChangedEvent(playerId, personId, oldScore, rel.getScore(), "Geschenk"));
        checkNetworkThresholds(playerId, personId, oldScore, rel.getScore(), character.getCurrentTurn());

        return new ActionResultDto(rel.getScore(), "Geschenk an " + person.name() + " (" + cost + "€). +" + SCORE_PER_GIFT + " Beziehungspunkte.");
    }

    @Transactional
    public ActionResultDto insult(Long playerId, String personId) {
        GameContext ctx = gameContextFactory.build(playerId);
        PersonDef person = requirePerson(personId);

        PlayerSocialRelationship rel = getOrCreateRelationship(playerId, personId, ctx.character().getCurrentTurn());
        requireNotLocked(rel, ctx.character().getCurrentTurn());

        if (rel.isMonthlyInsultDone()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Du hast diese Person diesen Monat bereits beleidigt.");
        }

        int oldScore = rel.getScore();
        rel.setScore(Math.max(0, oldScore + SCORE_INSULT));
        rel.setMonthlyInsultDone(true);
        rel.setHadConflict(true);
        relationshipRepo.save(rel);

        eventPublisher.publishEvent(new RelationshipChangedEvent(playerId, personId, oldScore, rel.getScore(), "Beleidigung"));

        return new ActionResultDto(rel.getScore(), person.name() + " beleidigt. " + SCORE_INSULT + " Beziehungspunkte.");
    }

    @Transactional
    public RobResultDto rob(Long playerId, String personId) {
        GameContext ctx = gameContextFactory.build(playerId);
        PersonDef person = requirePerson(personId);
        requireConditionMet(person, ctx);

        PlayerSocialRelationship rel = getOrCreateRelationship(playerId, personId, ctx.character().getCurrentTurn());
        requireNotLocked(rel, ctx.character().getCurrentTurn());

        if (rel.isMonthlyRobAttempted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Du hast diesen Monat bereits versucht, diese Person auszurauben.");
        }

        rel.setMonthlyRobAttempted(true);

        double robSuccessBoost = getBoostValue("ROB_SUCCESS_BOOST", relationshipRepo.findByPlayerId(playerId));
        double successChance = 0.20 + (rel.getScore() / 100.0 * 0.30) + robSuccessBoost;
        boolean success = Math.random() < successChance;

        GameCharacter character = ctx.character();
        int currentTurn = character.getCurrentTurn();

        if (success) {
            double robLootMultiplier = 1.0 + getBoostValue("ROB_LOOT_MULTIPLIER", relationshipRepo.findByPlayerId(playerId));
            int baseWealth = WEALTH_BASE[Math.min(person.wealthLevel(), WEALTH_BASE.length - 1)];
            BigDecimal loot = BigDecimal.valueOf(baseWealth * 0.10 * robLootMultiplier);
            character.setCash(character.getCash().add(loot));
            characterRepository.save(character);

            rel.setHadConflict(true);
            relationshipRepo.save(rel);
            eventPublisher.publishEvent(new RobAttemptedEvent(playerId, personId, true, false, loot));

            return new RobResultDto(true, false, loot,
                "Erfolgreich ausgeraubt! +" + loot.toPlainString() + "€");
        } else {
            boolean caught = Math.random() < 0.50;
            if (caught) {
                applyRobCaughtPenalties(playerId, personId, person, currentTurn);
                eventPublisher.publishEvent(new RobAttemptedEvent(playerId, personId, false, true, BigDecimal.ZERO));
                return new RobResultDto(false, true, BigDecimal.ZERO,
                    "Erwischt! Dein Ruf in der Gruppe ist stark beschädigt.");
            } else {
                rel.setHadConflict(true);
                relationshipRepo.save(rel);
                eventPublisher.publishEvent(new RobAttemptedEvent(playerId, personId, false, false, BigDecimal.ZERO));
                return new RobResultDto(false, false, BigDecimal.ZERO,
                    "Gescheitert, aber nicht erwischt.");
            }
        }
    }

    private void applyRobCaughtPenalties(Long playerId, String victimPersonId, PersonDef person, int currentTurn) {
        int lockUntil = currentTurn + ROB_CAUGHT_LOCK_MONTHS;

        PlayerSocialRelationship victimRel = relationshipRepo.findById(playerId, victimPersonId).orElse(null);
        if (victimRel != null) {
            victimRel.setScore(Math.max(0, victimRel.getScore() + ROB_CAUGHT_VICTIM_PENALTY));
            victimRel.setLockedActionsUntilTurn(lockUntil);
            victimRel.setHadConflict(true);
            relationshipRepo.save(victimRel);
        }

        Set<String> affectedGroupIds = new HashSet<>(person.groupIds());
        for (String groupId : affectedGroupIds) {
            for (PersonDef groupMember : personService.getPersonsInGroup(groupId)) {
                if (groupMember.id().equals(victimPersonId)) continue;
                PlayerSocialRelationship memberRel = relationshipRepo.findById(playerId, groupMember.id()).orElse(null);
                if (memberRel == null) continue;
                memberRel.setScore(Math.max(0, memberRel.getScore() + ROB_CAUGHT_GROUP_PENALTY));
                memberRel.setLockedActionsUntilTurn(lockUntil);
                relationshipRepo.save(memberRel);
            }
            eventPublisher.publishEvent(new SocialActionLockAppliedEvent(playerId, groupId, lockUntil));
        }
    }

    // ---- Monthly advance (called by TurnService) ----

    @Transactional
    public void advanceSocials(Long playerId) {
        List<PlayerSocialRelationship> relationships = relationshipRepo.findByPlayerId(playerId);
        for (PlayerSocialRelationship rel : relationships) {
            rel.setScore(Math.max(0, rel.getScore() - PASSIVE_DECAY_PER_MONTH));
            rel.setMonthlyTimeSpentCount(0);
            rel.setMonthlyInsultDone(false);
            rel.setMonthlyRobAttempted(false);
            relationshipRepo.save(rel);
        }
    }

    public List<ActiveBoostDto> getActiveBoosts(Long playerId) {
        List<PlayerSocialRelationship> relationships = relationshipRepo.findByPlayerId(playerId);
        Map<String, PlayerSocialRelationship> relMap = relationships.stream()
            .collect(Collectors.toMap(PlayerSocialRelationship::getPersonId, r -> r));
        return computeActiveBoosts(relMap);
    }

    public double getBoostValueForPlayer(Long playerId, String boostType) {
        return getBoostValue(boostType, relationshipRepo.findByPlayerId(playerId));
    }

    private double getBoostValue(String boostType, List<PlayerSocialRelationship> relationships) {
        double total = 0;
        for (PlayerSocialRelationship rel : relationships) {
            PersonDef p = personService.findById(rel.getPersonId()).orElse(null);
            if (p == null || p.boost() == null) continue;
            if (!boostType.equals(p.boost().type())) continue;
            total += p.boost().value() * (rel.getScore() / 100.0);
        }
        return total;
    }

    // ---- Network threshold check ----

    private void checkNetworkThresholds(Long playerId, String personId, int oldScore, int newScore, int currentTurn) {
        PersonDef person = personService.findById(personId).orElse(null);
        if (person == null) return;
        if (oldScore < person.networkThreshold() && newScore >= person.networkThreshold()) {
            for (String friendId : person.friendNetwork()) {
                PersonDef friend = personService.findById(friendId).orElse(null);
                if (friend == null) continue;
                if (!relationshipRepo.findById(playerId, friendId).isPresent()) {
                    for (String groupId : friend.groupIds()) {
                        if (!groupUnlockRepo.exists(playerId, groupId)) {
                            GroupDef g = personService.findGroupById(groupId).orElse(null);
                            if (g != null) {
                                groupUnlockRepo.save(new PlayerSocialGroupUnlock(playerId, groupId, currentTurn));
                                eventPublisher.publishEvent(new GroupUnlockedEvent(playerId, groupId, g.name()));
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Helpers ----

    private PersonDef requirePerson(String personId) {
        return personService.findById(personId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person nicht gefunden: " + personId));
    }

    private void requireConditionMet(PersonDef person, GameContext ctx) {
        if (person.unlockCondition() != null && !person.unlockCondition().isMet(ctx)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Voraussetzung nicht erfüllt: " + person.unlockCondition().describe());
        }
    }

    private void requireNotLocked(PlayerSocialRelationship rel, int currentTurn) {
        if (rel.getLockedActionsUntilTurn() > currentTurn) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Aktionen gesperrt bis Monat " + rel.getLockedActionsUntilTurn() + ".");
        }
    }

    private PlayerSocialRelationship getOrCreateRelationship(Long playerId, String personId, int currentTurn) {
        return relationshipRepo.findById(playerId, personId)
            .orElseGet(() -> {
                PlayerSocialRelationship rel = new PlayerSocialRelationship(playerId, personId, currentTurn);
                return relationshipRepo.save(rel);
            });
    }

    // ---- DTOs (inner records) ----

    public record SocialNetworkDto(
        List<PersonNodeDto> persons,
        List<PersonEdgeDto> edges,
        List<GroupDto> unlockedGroups,
        List<ActiveBoostDto> activeBoosts
    ) {}

    public record PersonNodeDto(
        String personId,
        String displayName,
        String groupId,
        boolean met,
        boolean known,
        int score,
        boolean locked,
        int lockedUntilTurn,
        boolean canSpendTime,
        boolean canInsult,
        boolean canRob,
        BoostDef boost,
        List<String> unlockRequirements
    ) {}

    public record PersonEdgeDto(String sourcePersonId, String targetPersonId) {}

    public record GroupDto(String id, String name, String type) {}

    public record ActiveBoostDto(String type, double totalValue) {}

    public record ActionResultDto(int newScore, String message) {}

    public record RobResultDto(boolean success, boolean caught, BigDecimal lootAmount, String message) {}
}
