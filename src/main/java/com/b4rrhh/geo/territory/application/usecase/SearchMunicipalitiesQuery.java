package com.b4rrhh.geo.territory.application.usecase;

import java.time.LocalDate;

/** {@code date} nula es hoy; {@code limit} nulo, veinte. */
public record SearchMunicipalitiesQuery(
        String ruleSystemCode,
        String countryCode,
        String name,
        LocalDate date,
        Integer limit
) {
}
