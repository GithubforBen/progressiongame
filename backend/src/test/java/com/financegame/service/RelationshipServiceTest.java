package com.financegame.service;

import com.financegame.entity.GameCharacter;
import com.financegame.entity.Npc;
import com.financegame.entity.PlayerRelationship;
import com.financegame.repository.CharacterRepository;
import com.financegame.repository.NpcRepository;
import com.financegame.repository.PlayerRelationshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {

    @Mock NpcRepository npcRepository;
    @Mock PlayerRelationshipRepository relationshipRepository;
    @Mock CharacterRepository characterRepository;

    RelationshipService service;

    @BeforeEach
    void setUp() {
        service = new RelationshipService(npcRepository, relationshipRepository, characterRepository);
    }

    // ── advanceRelationships ─────────────────────────────────────────────────

    @Test
    void advanceRelationships_noRelationships_returnsZeroBonus() {
        when(relationshipRepository.findByPlayerId(1L)).thenReturn(List.of());

        int bonus = service.advanceRelationships(1L, new ArrayList<>());

        assertThat(bonus).isZero();
    }

    @Test
    void advanceRelationships_incrementsLevelByOne() {
        PlayerRelationship rel = relationship(10L, 50);
        when(relationshipRepository.findByPlayerId(1L)).thenReturn(List.of(rel));
        when(npcRepository.findAll()).thenReturn(List.of(npc(10L, 2)));
        when(relationshipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.advanceRelationships(1L, new ArrayList<>());

        assertThat(rel.getLevel()).isEqualTo(51);
        assertThat(rel.getMonthsKnown()).isEqualTo(1);
    }

    @Test
    void advanceRelationships_levelCappedAt100() {
        PlayerRelationship rel = relationship(10L, 100);
        when(relationshipRepository.findByPlayerId(1L)).thenReturn(List.of(rel));
        when(npcRepository.findAll()).thenReturn(List.of(npc(10L, 2)));
        when(relationshipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.advanceRelationships(1L, new ArrayList<>());

        assertThat(rel.getLevel()).isEqualTo(100);
    }

    @Test
    void advanceRelationships_happinessBonusFormula() {
        // Bonus = round(level * happinessBonusPerLevel / 100)
        // Level 50, bonus-per-level 2 → round(50*2/100) = round(1.0) = 1
        PlayerRelationship rel = relationship(10L, 50);
        when(relationshipRepository.findByPlayerId(1L)).thenReturn(List.of(rel));
        when(npcRepository.findAll()).thenReturn(List.of(npc(10L, 2)));
        when(relationshipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        int bonus = service.advanceRelationships(1L, new ArrayList<>());

        // After advance, level is 51 → round(51*2/100) = round(1.02) = 1
        assertThat(bonus).isEqualTo(1);
    }

    @Test
    void advanceRelationships_maxLevel_maxBonus() {
        // Level 100, bonus-per-level 3 → round(100*3/100) = 3
        PlayerRelationship rel = relationship(10L, 100);
        when(relationshipRepository.findByPlayerId(1L)).thenReturn(List.of(rel));
        when(npcRepository.findAll()).thenReturn(List.of(npc(10L, 3)));
        when(relationshipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        int bonus = service.advanceRelationships(1L, new ArrayList<>());

        assertThat(bonus).isEqualTo(3);
    }

    @Test
    void advanceRelationships_multipleRelationships_sumsBonuses() {
        // Two level-100 relationships with bonus 2 each → 2 + 2 = 4
        PlayerRelationship rel1 = relationship(10L, 100);
        PlayerRelationship rel2 = relationship(20L, 100);
        when(relationshipRepository.findByPlayerId(1L)).thenReturn(List.of(rel1, rel2));
        when(npcRepository.findAll()).thenReturn(List.of(npc(10L, 2), npc(20L, 2)));
        when(relationshipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        int bonus = service.advanceRelationships(1L, new ArrayList<>());

        assertThat(bonus).isEqualTo(4);
    }

    // ── meet ─────────────────────────────────────────────────────────────────

    @Test
    void meet_alreadyKnown_throwsConflict() {
        when(npcRepository.findById(5L)).thenReturn(Optional.of(npc(5L, 2)));
        when(relationshipRepository.findByPlayerIdAndNpcId(1L, 5L))
            .thenReturn(Optional.of(relationship(5L, 0)));

        assertThatThrownBy(() -> service.meet(1L, 5L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("bereits");
    }

    @Test
    void meet_newNpc_createsRelationshipAtLevel0() {
        when(npcRepository.findById(5L)).thenReturn(Optional.of(npc(5L, 2)));
        when(relationshipRepository.findByPlayerIdAndNpcId(1L, 5L)).thenReturn(Optional.empty());
        when(relationshipRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        stubCurrentTurn(3);

        var result = service.meet(1L, 5L);

        assertThat(result.met()).isTrue();
        assertThat(result.level()).isZero();
    }

    // ── interact ─────────────────────────────────────────────────────────────

    @Test
    void interact_alreadyInteractedThisTurn_throwsConflict() {
        stubCurrentTurn(5);
        when(npcRepository.findById(5L)).thenReturn(Optional.of(npc(5L, 2)));
        PlayerRelationship rel = relationship(5L, 30);
        rel.setLastInteractedTurn(5); // same as current turn
        when(relationshipRepository.findByPlayerIdAndNpcId(1L, 5L)).thenReturn(Optional.of(rel));

        assertThatThrownBy(() -> service.interact(1L, 5L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("bereits");
    }

    @Test
    void interact_valid_increases10Levels() {
        stubCurrentTurn(5);
        when(npcRepository.findById(5L)).thenReturn(Optional.of(npc(5L, 2)));
        PlayerRelationship rel = relationship(5L, 30);
        rel.setLastInteractedTurn(4);
        when(relationshipRepository.findByPlayerIdAndNpcId(1L, 5L)).thenReturn(Optional.of(rel));
        when(relationshipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.interact(1L, 5L);

        assertThat(result.level()).isEqualTo(40);
        assertThat(result.canInteract()).isFalse(); // lastInteractedTurn now == currentTurn
    }

    @Test
    void interact_levelCappedAt100() {
        stubCurrentTurn(5);
        when(npcRepository.findById(5L)).thenReturn(Optional.of(npc(5L, 2)));
        PlayerRelationship rel = relationship(5L, 95);
        rel.setLastInteractedTurn(4);
        when(relationshipRepository.findByPlayerIdAndNpcId(1L, 5L)).thenReturn(Optional.of(rel));
        when(relationshipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.interact(1L, 5L);

        assertThat(result.level()).isEqualTo(100);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubCurrentTurn(int turn) {
        GameCharacter c = new GameCharacter();
        c.setPlayerId(1L);
        c.setCash(BigDecimal.valueOf(1000));
        c.setCurrentTurn(turn);
        when(characterRepository.findByPlayerId(1L)).thenReturn(Optional.of(c));
    }

    private PlayerRelationship relationship(Long npcId, int level) {
        PlayerRelationship rel = new PlayerRelationship(1L, npcId);
        rel.setLevel(level);
        return rel;
    }

    private Npc npc(Long id, int bonusPerLevel) {
        // Use reflection-free approach via a subclass since Npc has no setters for id
        return new Npc() {
            @Override public Long getId() { return id; }
            @Override public String getName() { return "TestNPC"; }
            @Override public String getDescription() { return "Test"; }
            @Override public String getPersonality() { return "FRIENDLY"; }
            @Override public int getHappinessBonusPerLevel() { return bonusPerLevel; }
        };
    }
}
