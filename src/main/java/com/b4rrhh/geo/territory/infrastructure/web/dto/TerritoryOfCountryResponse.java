package com.b4rrhh.geo.territory.infrastructure.web.dto;

public record TerritoryOfCountryResponse(
        String countryCode,
        boolean hasTerritory,
        String message
) {
}
