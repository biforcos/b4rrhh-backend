package com.b4rrhh.rulesystem.company.application.usecase;

public record UpdateCompanyCommand(
        String ruleSystemCode,
        String companyCode,
        String name,
        String description,
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
