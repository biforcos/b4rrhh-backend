package com.b4rrhh.employee.employee.domain.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}