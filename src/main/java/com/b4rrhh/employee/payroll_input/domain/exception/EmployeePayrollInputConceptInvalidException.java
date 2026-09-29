package com.b4rrhh.employee.payroll_input.domain.exception;

/** El concepto no existe, o existe y se calcula: no es de entrada (b4rrhh/backend#142). */
public class EmployeePayrollInputConceptInvalidException extends RuntimeException {

    public EmployeePayrollInputConceptInvalidException(String message) {
        super(message);
    }
}
