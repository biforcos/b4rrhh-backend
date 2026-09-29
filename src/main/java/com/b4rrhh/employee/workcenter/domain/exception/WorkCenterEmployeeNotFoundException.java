package com.b4rrhh.employee.workcenter.domain.exception;

public class WorkCenterEmployeeNotFoundException extends RuntimeException {

    public WorkCenterEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}