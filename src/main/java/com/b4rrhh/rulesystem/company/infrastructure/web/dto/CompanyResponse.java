package com.b4rrhh.rulesystem.company.infrastructure.web.dto;

import java.time.LocalDate;

public record CompanyResponse(
        String ruleSystemCode,
        String companyCode,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        String legalName,
        String taxIdentifier,
        CompanyAddressResponse address,
        String cnaeCode
) {
}
