package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.geo.address.domain.model.PostalAddressInput;

public record UpdateCompanyCommand(
        String ruleSystemCode,
        String companyCode,
        String name,
        String description,
        String legalName,
        String taxIdentifier,
        PostalAddressInput address,
        String cnaeCode
) {
}
