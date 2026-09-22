package com.b4rrhh.rulesystem.companyprofile.application.usecase;

public record UpsertCompanyProfileCommand(
        String ruleSystemCode,
        String companyCode,
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