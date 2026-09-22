package com.b4rrhh.rulesystem.company.infrastructure.web.dto;

public record UpdateCompanyRequest(
        String name,
        String description,
        String legalName,
        String taxIdentifier,
        CompanyAddressRequest address,
        String cnaeCode
) {
}
