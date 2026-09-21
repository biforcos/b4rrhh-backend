package com.b4rrhh.employee.extra_payment_regime.domain.exception;

public class ExtraPaymentRegimeEmployeeNotFoundException extends RuntimeException {

    public ExtraPaymentRegimeEmployeeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("Employee not found for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber);
    }
}