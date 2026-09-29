package com.b4rrhh.employee.labor_classification.domain.exception;

public class LaborClassificationEmployeeNotFoundException extends RuntimeException {

    public LaborClassificationEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
