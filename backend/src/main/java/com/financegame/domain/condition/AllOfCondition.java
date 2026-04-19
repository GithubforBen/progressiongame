package com.financegame.domain.condition;

import com.financegame.domain.GameContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Passes only when ALL sub-conditions pass (logical AND). */
public class AllOfCondition implements Condition {

    private final List<Condition> conditions;

    public AllOfCondition(Condition... conditions) {
        this.conditions = Arrays.asList(conditions);
    }

    @Override
    public boolean isMet(GameContext context) {
        return conditions.stream().allMatch(c -> c.isMet(context));
    }

    @Override
    public String describe() {
        return conditions.stream()
            .map(Condition::describe)
            .collect(Collectors.joining(" und "));
    }
}
