package com.b4rrhh.employee.labor_classification.domain.exception;

import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;

import java.util.List;

/**
 * The resulting series would leave a stretch of the employee's presence
 * without a labor classification (ADR-057): the coverage of this series is
 * mandatory. It names the gaps and the neighbouring occurrences the user
 * could stretch to cover them.
 */
public class LaborClassificationCoverageIncompleteException extends RuntimeException {

    private final List<LaborClassificationPeriod> gaps;
    private final List<LaborClassificationPeriod> stretchCandidates;

    public LaborClassificationCoverageIncompleteException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, List.of(), List.of());
    }

    public LaborClassificationCoverageIncompleteException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            List<LaborClassificationPeriod> gaps,
            List<LaborClassificationPeriod> stretchCandidates
    ) {
        super("Labor classification coverage is incomplete for ruleSystemCode="
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

    public List<LaborClassificationPeriod> gaps() {
        return gaps;
    }

    public List<LaborClassificationPeriod> stretchCandidates() {
        return stretchCandidates;
    }
}
