package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.geo.address.domain.model.PostalAddressInput;

public record UpdateWorkCenterCommand(
        String ruleSystemCode,
        String workCenterCode,
        String name,
        String description,
        String companyCode,
        PostalAddressInput address
) {
}