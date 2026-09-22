package com.b4rrhh.rulesystem.company.application.usecase;

import java.time.LocalDate;

public record CreateCompanyCommand(
        String ruleSystemCode,
        String companyCode,
        String name,
        String description,
        LocalDate startDate,
        String legalName,
        String taxIdentifier,
        String street,
        String city,
        String postalCode,
        String regionCode,
        String countryCode,
        String cnaeCode
) {
}
