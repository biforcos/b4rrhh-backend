package com.b4rrhh.employee.absence.application.usecase;

import java.time.LocalDate;

public record GetAbsenceByBusinessKeyCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber,
    String absenceTypeCode, LocalDate startDate, int startTime) {}
