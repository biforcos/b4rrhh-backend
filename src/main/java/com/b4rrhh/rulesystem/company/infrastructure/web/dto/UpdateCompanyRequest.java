package com.b4rrhh.rulesystem.company.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressRequest;

public record UpdateCompanyRequest(
        String name,
        String description,
        String legalName,
        String taxIdentifier,
        PostalAddressRequest address,
        String cnaeCode
) {
}
