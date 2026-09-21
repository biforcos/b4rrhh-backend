package com.b4rrhh.employee.extra_payment_regime.infrastructure.persistence;

import com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimeAgreementContext;
import com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimeAgreementContextLookupPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Resolves the agreement context (ruleSystemCode + agreementCode) for an employee at a given date
 * by looking up the valid labor classification record.
 *
 * Uses JPA repository temporal query ordered by startDate descending.
 */
@Component
public class ExtraPaymentRegimeAgreementContextLookupAdapter implements ExtraPaymentRegimeAgreementContextLookupPort {

    private final ExtraPaymentRegimeAgreementContextRepository repository;

    public ExtraPaymentRegimeAgreementContextLookupAdapter(ExtraPaymentRegimeAgreementContextRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExtraPaymentRegimeAgreementContext resolveContext(Long employeeId, LocalDate effectiveDate) {
        return repository.findLatestValidByEmployeeIdAndEffectiveDate(
                        employeeId,
                        effectiveDate,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No valid labor classification found for employee " + employeeId + " at " + effectiveDate
                ));
    }
}
