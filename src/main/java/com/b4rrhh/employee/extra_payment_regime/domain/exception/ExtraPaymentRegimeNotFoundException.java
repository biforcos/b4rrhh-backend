package com.b4rrhh.employee.extra_payment_regime.domain.exception;

public class ExtraPaymentRegimeNotFoundException extends RuntimeException {

    public ExtraPaymentRegimeNotFoundException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            Integer extraPaymentRegimeNumber
    ) {
        super("Extra payment regime not found for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", extraPaymentRegimeNumber="
                + extraPaymentRegimeNumber);
    }
}