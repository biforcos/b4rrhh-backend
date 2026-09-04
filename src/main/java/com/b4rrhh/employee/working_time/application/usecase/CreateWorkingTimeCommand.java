package com.b4rrhh.employee.working_time.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateWorkingTimeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal workingTimePercentage
) {
}