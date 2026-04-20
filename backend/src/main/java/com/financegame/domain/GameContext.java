package com.financegame.domain;

import com.financegame.entity.GameCharacter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Snapshot of all player state needed to evaluate Conditions and apply Effects
 * during a single request or turn. Built via GameContextFactory.
 */
public record GameContext(
    GameCharacter character,
    List<String> completedEducationStages,
    String currentCountry,
    boolean traveling,
    int activeJobCount,
    Set<String> completedCollections,
    Map<String, Integer> relationshipScores,
    Set<String> hadConflictsWith
) {}
