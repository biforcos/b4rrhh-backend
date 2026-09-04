package com.b4rrhh.employee.working_time.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateWorkingTimeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer workingTimeNumber,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal workingTimePercentage
) {}
