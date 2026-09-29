package com.b4rrhh.payroll.year.domain.exception;

public class EmployeeYearEmployeeNotFoundException extends RuntimeException {

    public EmployeeYearEmployeeNotFoundException(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        super("Employee not found: " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber);
    }
}
