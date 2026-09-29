package com.b4rrhh.employee.extra_payment_regime.domain.exception;

public class ExtraPaymentRegimeEmployeeNotFoundException extends RuntimeException {

    public ExtraPaymentRegimeEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}