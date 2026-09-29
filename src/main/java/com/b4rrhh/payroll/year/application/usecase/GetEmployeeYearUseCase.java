package com.b4rrhh.payroll.year.application.usecase;

import com.b4rrhh.payroll.year.application.model.EmployeeYear;

public interface GetEmployeeYearUseCase {

    EmployeeYear get(GetEmployeeYearCommand command);
}
