package com.b4rrhh.employee.extra_payment_regime.application.port;

import java.time.LocalDate;

/**
 * Resolves the agreement context for an employee at a specific effective date.
 * Responsible for: given employeeId + effectiveDate → resolve the valid labor classification
 * and return the business keys identifying the applicable collective agreement.
 */
public interface ExtraPaymentRegimeAgreementContextLookupPort {

    /**
     * Resolve the agreement context for an employee at the given effective date.
     *
     * @param employeeId    internal employee ID
     * @param effectiveDate date for which to find the valid labor classification
     * @return agreement context with ruleSystemCode and agreementCode
     * @throws IllegalStateException if no valid labor classification is found at that date
     */
    ExtraPaymentRegimeAgreementContext resolveContext(Long employeeId, LocalDate effectiveDate);
}
