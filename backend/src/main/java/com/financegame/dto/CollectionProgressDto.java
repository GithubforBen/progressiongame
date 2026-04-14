package com.financegame.dto;

public record CollectionProgressDto(
    String collectionType,
    int total,
    int owned,
    double percentage
) {}
