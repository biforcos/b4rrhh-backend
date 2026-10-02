package com.b4rrhh.geo.territory.infrastructure.web.dto;

public record TerritoryPostalCodeResponse(
        String postalCode,
        String countryCode,
        TerritoryProvinceRefResponse province,
        TerritoryMunicipalityRefResponse suggestedMunicipality,
        String message
) {
}
