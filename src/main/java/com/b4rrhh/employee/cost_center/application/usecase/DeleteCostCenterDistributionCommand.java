package com.b4rrhh.employee.cost_center.application.usecase;

import java.time.LocalDate;

/** The distribution window to remove, named by the day it starts. */
public record DeleteCostCenterDistributionCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate windowStartDate
) {
}
