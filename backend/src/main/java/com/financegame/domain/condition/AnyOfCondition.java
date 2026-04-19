package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Passes when AT LEAST ONE sub-condition passes (logical OR). */
public class AnyOfCondition implements Condition {

    private final List<Condition> conditions;

    public AnyOfCondition(Condition... conditions) {
        this.conditions = Arrays.asList(conditions);
    }

    @Override
    public boolean isMet(GameContext context) {
        return conditions.stream().anyMatch(c -> c.isMet(context));
    }

    @Override
    public String describe() {
        return conditions.stream()
            .map(Condition::describe)
            .collect(Collectors.joining(" oder "));
    }
}
