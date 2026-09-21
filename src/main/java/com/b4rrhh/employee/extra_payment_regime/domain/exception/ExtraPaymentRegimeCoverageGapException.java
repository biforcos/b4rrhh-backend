package com.b4rrhh.employee.extra_payment_regime.domain.exception;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimeOccurrence;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimePeriod;

import java.util.List;

/**
 * The resulting series would leave a stretch of the employee's presence
 * without a extra payment regime (ADR-057): the coverage of this series is
 * mandatory. It names the gaps and the neighbouring occurrences the user
 * could stretch to cover them.
 */
public final class ExtraPaymentRegimeCoverageGapException extends ExtraPaymentRegimeSeriesInvariantException {

    private final List<ExtraPaymentRegimePeriod> gaps;
    private final List<ExtraPaymentRegimeOccurrence> stretchCandidates;

    public ExtraPaymentRegimeCoverageGapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            List<ExtraPaymentRegimePeriod> gaps,
            List<ExtraPaymentRegimeOccurrence> stretchCandidates
    ) {
        super("Extra payment regime series would leave the presence uncovered for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", gaps="
                + gaps
                + ", stretchCandidates="
                + stretchCandidates);
        this.gaps = List.copyOf(gaps);
        this.stretchCandidates = List.copyOf(stretchCandidates);
    }

    public List<ExtraPaymentRegimePeriod> gaps() {
        return gaps;
    }

    public List<ExtraPaymentRegimeOccurrence> stretchCandidates() {
        return stretchCandidates;
    }
}
