package com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressResponse;

public record CompanyProfileResponse(
        String companyCode,
        String legalName,
        String taxIdentifier,
        PostalAddressResponse address,
        String cnaeCode
) {
}