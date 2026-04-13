package com.financegame.dto;

import com.financegame.entity.Country;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public record CountryDto(
    Long id,
    String name,
    BigDecimal travelCost,
    int travelMonths,
    String emoji,
    String description,
    boolean visited,
    boolean currentlyHere,
    boolean travelingHere
) {
    public static CountryDto from(Country c, String currentCountry, String destination, String[] visited) {
        List<String> visitedList = Arrays.asList(visited);
        return new CountryDto(
            c.getId(),
            c.getName(),
            c.getTravelCost(),
            c.getTravelMonths(),
            c.getEmoji(),
            c.getDescription(),
            visitedList.contains(c.getName()),
            c.getName().equals(currentCountry),
            c.getName().equals(destination)
        );
    }
}
