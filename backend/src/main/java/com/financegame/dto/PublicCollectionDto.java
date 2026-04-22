package com.financegame.dto;

public record PublicCollectionDto(
    String name,
    String displayName,
    int itemCount,
    int ownedCount,
    boolean completed
) {}
