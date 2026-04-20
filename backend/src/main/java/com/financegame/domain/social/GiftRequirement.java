package com.financegame.domain.social;

import com.financegame.domain.condition.Condition;

public record GiftRequirement(int cost, Condition condition, String description) {}
