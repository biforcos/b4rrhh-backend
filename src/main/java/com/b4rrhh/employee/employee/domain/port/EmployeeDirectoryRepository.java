package com.b4rrhh.employee.employee.domain.port;

import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryPage;

public interface EmployeeDirectoryRepository {

    /** La página pedida y el total de los que cumplen los mismos filtros. */
    EmployeeDirectoryPage findDirectoryByFilters(
            String q,
            String ruleSystemCode,
            String employeeTypeCode,
            String status,
            int page,
            int size
    );
}