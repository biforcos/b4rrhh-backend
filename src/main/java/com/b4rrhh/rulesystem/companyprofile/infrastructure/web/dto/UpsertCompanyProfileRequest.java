package com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressRequest;

public record UpsertCompanyProfileRequest(
        String legalName,
        String taxIdentifier,
        PostalAddressRequest address,
        String cnaeCode
) {
}