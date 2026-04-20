package com.financegame.domain.social;

import com.financegame.domain.condition.Condition;

import java.util.List;

public record PersonDef(
    String id,
    String name,
    String description,
    List<String> groupIds,
    int networkThreshold,
    List<String> friendNetwork,
    BoostDef boost,
    Condition unlockCondition,
    GiftRequirement giftRequirement,
    int wealthLevel
) {}
