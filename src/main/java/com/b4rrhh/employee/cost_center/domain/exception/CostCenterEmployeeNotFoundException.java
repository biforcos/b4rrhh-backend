package com.b4rrhh.employee.cost_center.domain.exception;

public class CostCenterEmployeeNotFoundException extends RuntimeException {

    public CostCenterEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}
