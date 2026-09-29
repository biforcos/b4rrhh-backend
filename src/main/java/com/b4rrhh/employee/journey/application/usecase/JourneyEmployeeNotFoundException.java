package com.b4rrhh.employee.journey.application.usecase;

public class JourneyEmployeeNotFoundException extends RuntimeException {

    public JourneyEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
