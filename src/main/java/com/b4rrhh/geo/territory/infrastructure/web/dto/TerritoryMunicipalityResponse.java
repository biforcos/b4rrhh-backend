package com.b4rrhh.geo.territory.infrastructure.web.dto;

import java.time.LocalDate;

public record TerritoryMunicipalityResponse(
        String code,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        TerritoryProvinceRefResponse province,
        TerritoryRegionRefResponse region
) {
}
