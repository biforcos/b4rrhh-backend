package com.b4rrhh.employee.lifecycle.domain.exception;

public class RehireEmployeeEmployeeNotFoundException extends RuntimeException {

    public RehireEmployeeEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}