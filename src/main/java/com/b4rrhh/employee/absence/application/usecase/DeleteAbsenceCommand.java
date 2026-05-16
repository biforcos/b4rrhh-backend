package com.b4rrhh.employee.absence.application.usecase;

import java.time.LocalDate;

public record DeleteAbsenceCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber,
    String absenceTypeCode, LocalDate startDate, int startTime) {}
