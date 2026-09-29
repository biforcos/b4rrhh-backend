package com.b4rrhh.payroll.retro.domain.exception;

public class RetroMarkEmployeeNotFoundException extends RuntimeException {

    public RetroMarkEmployeeNotFoundException(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
