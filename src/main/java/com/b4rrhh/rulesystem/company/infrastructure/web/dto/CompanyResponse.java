package com.b4rrhh.rulesystem.company.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressResponse;
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
        PostalAddressResponse address,
        String cnaeCode
) {
}
