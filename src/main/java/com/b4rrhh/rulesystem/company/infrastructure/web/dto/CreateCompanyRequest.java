package com.b4rrhh.rulesystem.company.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressRequest;
import java.time.LocalDate;

public record CreateCompanyRequest(
        String ruleSystemCode,
        String companyCode,
        String name,
        String description,
        LocalDate startDate,
        String legalName,
        String taxIdentifier,
        PostalAddressRequest address,
        String cnaeCode
) {
}
