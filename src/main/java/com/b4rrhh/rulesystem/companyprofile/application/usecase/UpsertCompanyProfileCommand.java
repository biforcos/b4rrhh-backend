package com.b4rrhh.rulesystem.companyprofile.application.usecase;

import com.b4rrhh.geo.address.domain.model.PostalAddressInput;

public record UpsertCompanyProfileCommand(
        String ruleSystemCode,
        String companyCode,
        String legalName,
        String taxIdentifier,
        PostalAddressInput address,
        String cnaeCode
) {
}