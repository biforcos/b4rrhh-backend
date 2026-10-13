package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.geo.address.domain.model.PostalAddressInput;
import java.time.LocalDate;

public record CreateWorkCenterCommand(
        String ruleSystemCode,
        String workCenterCode,
        String name,
        String description,
        LocalDate startDate,
        String companyCode,
        PostalAddressInput address
) {
}