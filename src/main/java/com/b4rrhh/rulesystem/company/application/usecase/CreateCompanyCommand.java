package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.geo.address.domain.model.PostalAddressInput;
import java.time.LocalDate;

public record CreateCompanyCommand(
        String ruleSystemCode,
        String companyCode,
        String name,
        String description,
        LocalDate startDate,
        String legalName,
        String taxIdentifier,
        PostalAddressInput address,
        String cnaeCode
) {
}
