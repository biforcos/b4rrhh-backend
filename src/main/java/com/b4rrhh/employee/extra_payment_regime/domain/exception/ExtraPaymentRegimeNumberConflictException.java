package com.b4rrhh.employee.extra_payment_regime.domain.exception;

public class ExtraPaymentRegimeNumberConflictException extends RuntimeException {

    public ExtraPaymentRegimeNumberConflictException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            Integer extraPaymentRegimeNumber,
            Throwable cause
    ) {
        super("Extra payment regime number conflict for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", extraPaymentRegimeNumber="
                + extraPaymentRegimeNumber, cause);
    }
}