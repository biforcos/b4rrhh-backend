package com.b4rrhh.employee.contact.domain.exception;

public class ContactEmployeeNotFoundException extends RuntimeException {

    public ContactEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
