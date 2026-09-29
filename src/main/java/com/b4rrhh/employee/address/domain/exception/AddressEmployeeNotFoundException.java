package com.b4rrhh.employee.address.domain.exception;

public class AddressEmployeeNotFoundException extends RuntimeException {

    public AddressEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
