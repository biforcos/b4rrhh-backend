package com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto;

public record UpsertCompanyProfileRequest(
        String legalName,
        String taxIdentifier,
        CompanyProfileAddressRequest address,
        String cnaeCode
) {
}