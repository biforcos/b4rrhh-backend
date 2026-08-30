package com.b4rrhh.employee.employee.application.usecase;

import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryPage;

public interface ListEmployeesUseCase {

    EmployeeDirectoryPage list(ListEmployeesQuery query);
}