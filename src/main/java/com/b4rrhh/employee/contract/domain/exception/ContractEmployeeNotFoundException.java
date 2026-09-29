package com.b4rrhh.employee.contract.domain.exception;

public class ContractEmployeeNotFoundException extends RuntimeException {

    public ContractEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
