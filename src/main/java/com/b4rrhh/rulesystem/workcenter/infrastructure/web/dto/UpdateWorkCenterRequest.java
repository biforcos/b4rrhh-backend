package com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressRequest;

public record UpdateWorkCenterRequest(
        String name,
        String description,
        String companyCode,
        PostalAddressRequest address
) {
}