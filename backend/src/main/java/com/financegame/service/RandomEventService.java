package com.financegame.service;

import com.financegame.domain.event.RandomGameEvent;
import com.financegame.entity.GameCharacter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Iterates all registered RandomGameEvent strategies each turn.
 * Adding a new event type requires only a new @Component — no changes here.
 */
@Service
public class RandomEventService {

    private final List<RandomGameEvent> randomGameEvents;

    public RandomEventService(List<RandomGameEvent> randomGameEvents) {
        this.randomGameEvents = randomGameEvents;
    }

    public void applyRandomEvents(Long playerId, GameCharacter character, List<String> events) {
        for (RandomGameEvent event : randomGameEvents) {
            event.tryApply(playerId, character, events);
        }
    }
}
