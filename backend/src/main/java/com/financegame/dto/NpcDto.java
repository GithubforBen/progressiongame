package com.financegame.dto;

import com.financegame.entity.Npc;
import com.financegame.entity.PlayerRelationship;

public record NpcDto(
    Long id,
    String name,
    String description,
    String personality,
    int happinessBonusPerLevel,
    boolean met,
    int level,
    int monthsKnown,
    boolean canInteract
) {
    public static NpcDto from(Npc npc, PlayerRelationship rel, int currentTurn) {
        if (rel == null) {
            return new NpcDto(npc.getId(), npc.getName(), npc.getDescription(),
                              npc.getPersonality(), npc.getHappinessBonusPerLevel(),
                              false, 0, 0, false);
        }
        boolean canInteract = rel.getLastInteractedTurn() < currentTurn;
        return new NpcDto(npc.getId(), npc.getName(), npc.getDescription(),
                          npc.getPersonality(), npc.getHappinessBonusPerLevel(),
                          true, rel.getLevel(), rel.getMonthsKnown(), canInteract);
    }
}
