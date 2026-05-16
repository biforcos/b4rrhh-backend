package com.b4rrhh.employee.absence.application.usecase;

public record ListEmployeeAbsencesCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber) {}
