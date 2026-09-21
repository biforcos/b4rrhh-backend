package com.b4rrhh.employee.extra_payment_regime.application.port;

/**
 * Minimal agreement context resolved from an employee's labor classification at a given date.
 * Contains only the business keys needed to look up the agreement profile.
 */
public record ExtraPaymentRegimeAgreementContext(String ruleSystemCode, String agreementCode) {
}
