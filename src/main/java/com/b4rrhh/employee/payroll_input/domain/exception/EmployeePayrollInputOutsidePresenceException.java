package com.b4rrhh.employee.payroll_input.domain.exception;

/**
 * El período de la entrada no tiene ni un día de presencia: ningún recibo la consumiría
 * (b4rrhh/backend#142).
 */
public class EmployeePayrollInputOutsidePresenceException extends RuntimeException {

    public EmployeePayrollInputOutsidePresenceException(String message) {
        super(message);
    }
}
