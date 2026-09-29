package com.b4rrhh.employee.presence.domain.exception;

public class PresenceEmployeeNotFoundException extends RuntimeException {

    public PresenceEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
