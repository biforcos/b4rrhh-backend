package com.b4rrhh.geo.territory.domain.model;

import java.time.LocalDate;

/**
 * Un municipio del maestro geo. {@code startDate} es «vigente desde al menos» (ADR-078): la
 * fecha de la primera relación del INE cargada que lo trae, no la de su creación.
 */
public record Municipality(
        String countryCode,
        String code,
        String name,
        String provinceCode,
        LocalDate startDate,
        LocalDate endDate
) {
}
