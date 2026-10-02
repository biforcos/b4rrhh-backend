package com.b4rrhh.geo.territory.infrastructure.web.dto;

public record TerritoryProvinceResponse(
        String code,
        String name,
        String isoCode,
        TerritoryRegionResponse region
) {
}
