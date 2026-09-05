package com.b4rrhh.employee.labor_classification.application.command;

import java.time.LocalDate;

/** Removes the labor classification that starts on {@code startDate}: it is identified by the day it starts. */
public record DeleteLaborClassificationCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate startDate
) {
}
