package com.financegame.domain.event;

import com.financegame.entity.GameCharacter;

import java.util.List;

/**
 * Strategy for one type of random monthly event.
 * Implement as a Spring @Component; RandomEventService discovers all implementations.
 * Adding a new event type = adding one new @Component.
 */
public interface RandomGameEvent {

    /**
     * Attempt to fire this event. Each implementation rolls its own probability.
     *
     * @param playerId  the current player
     * @param character mutable character state (caller persists after all events)
     * @param events    message list for the turn result
     */
    void tryApply(Long playerId, GameCharacter character, List<String> events);
}
