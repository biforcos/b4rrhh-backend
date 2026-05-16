package com.b4rrhh.employee.absence.application.usecase;

import java.time.LocalDate;

public interface CloseOpenAbsenceAtTerminationUseCase {
    void closeIfOpen(String ruleSystemCode, String employeeTypeCode,
                     String employeeNumber, LocalDate terminationDate);
}
