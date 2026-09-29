package com.b4rrhh.employee.identifier.domain.exception;

public class IdentifierEmployeeNotFoundException extends RuntimeException {

    public IdentifierEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
