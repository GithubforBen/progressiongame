package com.financegame.domain.social;

import com.financegame.domain.condition.Condition;

public record GroupDef(
    String id,
    String name,
    String type,
    String description,
    Condition exclusiveCondition
) {}
