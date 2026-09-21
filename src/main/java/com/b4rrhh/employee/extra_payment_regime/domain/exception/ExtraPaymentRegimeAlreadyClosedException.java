package com.b4rrhh.employee.extra_payment_regime.domain.exception;

public class ExtraPaymentRegimeAlreadyClosedException extends RuntimeException {

    public ExtraPaymentRegimeAlreadyClosedException(Integer extraPaymentRegimeNumber) {
        super("Extra payment regime already closed for extraPaymentRegimeNumber=" + extraPaymentRegimeNumber);
    }
}