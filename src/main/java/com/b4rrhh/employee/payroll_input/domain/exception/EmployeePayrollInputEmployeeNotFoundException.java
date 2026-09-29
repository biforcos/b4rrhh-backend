package com.b4rrhh.employee.payroll_input.domain.exception;

public class EmployeePayrollInputEmployeeNotFoundException extends RuntimeException {

    public EmployeePayrollInputEmployeeNotFoundException(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
