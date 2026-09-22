package com.b4rrhh.rulesystem.company.infrastructure.web.dto;

import java.time.LocalDate;

public record CreateCompanyRequest(
        String ruleSystemCode,
        String companyCode,
        String name,
        String description,
        LocalDate startDate,
        String legalName,
        String taxIdentifier,
        CompanyAddressRequest address,
        String cnaeCode
) {
}
