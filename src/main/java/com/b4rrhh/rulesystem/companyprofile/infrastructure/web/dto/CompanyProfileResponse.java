package com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto;

public record CompanyProfileResponse(
        String companyCode,
        String legalName,
        String taxIdentifier,
        CompanyProfileAddressResponse address,
        String cnaeCode
) {
}