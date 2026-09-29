package com.b4rrhh.employee.presence.application.usecase;

import com.b4rrhh.employee.presence.domain.model.EmployeeStanding;

import java.time.LocalDate;

public interface GetEmployeeStandingUseCase {

    /** El estado del empleado en {@code date}, o en el día de hoy si no se da fecha (b4rrhh/backend#148). */
    EmployeeStanding standingOf(Long employeeId, LocalDate date);
}
