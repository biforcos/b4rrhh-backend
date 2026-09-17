package com.b4rrhh.payroll.application.port;

import com.b4rrhh.payroll.application.usecase.PayrollPeriodSegmentation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayrollLaunchWorkingTimeWindowContext(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal workingTimePercentage
) implements PayrollPeriodSegmentation.DatedWindow {
}
