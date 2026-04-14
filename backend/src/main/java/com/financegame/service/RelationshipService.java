package com.financegame.service;

import com.financegame.dto.NpcDto;
import com.financegame.entity.GameCharacter;
import com.financegame.entity.Npc;
import com.financegame.entity.PlayerRelationship;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.NpcRepository;
import com.financegame.repository.PlayerRelationshipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RelationshipService {

    private final NpcRepository npcRepository;
    private final PlayerRelationshipRepository relationshipRepository;
    private final CharacterRepository characterRepository;

    public RelationshipService(NpcRepository npcRepository,
                               PlayerRelationshipRepository relationshipRepository,
                               CharacterRepository characterRepository) {
        this.npcRepository = npcRepository;
        this.relationshipRepository = relationshipRepository;
        this.characterRepository = characterRepository;
    }

    @Transactional(readOnly = true)
    public List<NpcDto> getAll(Long playerId) {
        int currentTurn = currentTurn(playerId);
        Map<Long, PlayerRelationship> relMap = relationshipRepository.findByPlayerId(playerId)
            .stream().collect(Collectors.toMap(PlayerRelationship::getNpcId, Function.identity()));
        return npcRepository.findAll().stream()
            .map(npc -> NpcDto.from(npc, relMap.get(npc.getId()), currentTurn))
            .toList();
    }

    @Transactional
    public NpcDto meet(Long playerId, Long npcId) {
        Npc npc = npcRepository.findById(npcId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NPC nicht gefunden"));
        if (relationshipRepository.findByPlayerIdAndNpcId(playerId, npcId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Du kennst diese Person bereits.");
        }
        PlayerRelationship rel = new PlayerRelationship(playerId, npcId);
        relationshipRepository.save(rel);
        return NpcDto.from(npc, rel, currentTurn(playerId));
    }

    @Transactional
    public NpcDto interact(Long playerId, Long npcId) {
        int turn = currentTurn(playerId);
        Npc npc = npcRepository.findById(npcId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NPC nicht gefunden"));
        PlayerRelationship rel = relationshipRepository.findByPlayerIdAndNpcId(playerId, npcId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Du kennst diese Person noch nicht."));
        if (rel.getLastInteractedTurn() >= turn) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Du hast diese Person diesen Monat bereits getroffen.");
        }
        rel.setLevel(Math.min(100, rel.getLevel() + 10));
        rel.setLastInteractedTurn(turn);
        relationshipRepository.save(rel);
        return NpcDto.from(npc, rel, turn);
    }

    /**
     * Called from TurnService each month.
     * Advances all active relationships by +1 level passively.
     * Returns the total happiness bonus to apply to the character.
     */
    @Transactional
    public int advanceRelationships(Long playerId, List<String> events) {
        List<PlayerRelationship> rels = relationshipRepository.findByPlayerId(playerId);
        if (rels.isEmpty()) return 0;

        Map<Long, Npc> npcMap = npcRepository.findAll().stream()
            .collect(Collectors.toMap(Npc::getId, Function.identity()));

        int totalHappinessBonus = 0;
        for (PlayerRelationship rel : rels) {
            rel.setMonthsKnown(rel.getMonthsKnown() + 1);
            rel.setLevel(Math.min(100, rel.getLevel() + 1));
            relationshipRepository.save(rel);

            Npc npc = npcMap.get(rel.getNpcId());
            if (npc != null) {
                // Bonus: level * happinessBonusPerLevel / 100 (rounded)
                totalHappinessBonus += Math.round(rel.getLevel() * npc.getHappinessBonusPerLevel() / 100f);
            }
        }

        if (totalHappinessBonus > 0) {
            events.add("Beziehungen stärken deine Stimmung: +" + totalHappinessBonus + " Happiness.");
        }
        return totalHappinessBonus;
    }

    private int currentTurn(Long playerId) {
        return characterRepository.findByPlayerId(playerId)
            .map(GameCharacter::getCurrentTurn)
            .orElse(1);
    }
}
