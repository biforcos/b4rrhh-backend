package com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressRequest;
import java.time.LocalDate;

public record CreateWorkCenterRequest(
        String ruleSystemCode,
        String workCenterCode,
        String name,
        String description,
        LocalDate startDate,
        String companyCode,
        PostalAddressRequest address
) {
}