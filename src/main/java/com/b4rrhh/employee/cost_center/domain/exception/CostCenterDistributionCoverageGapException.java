package com.b4rrhh.employee.cost_center.domain.exception;

import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;

import java.util.List;

/**
 * The resulting series would leave a stretch of the employee's presence
 * without a cost center distribution (ADR-057): the coverage of this series
 * is mandatory. It names the gaps and the neighbouring windows the user could
 * stretch to cover them.
 */
public class CostCenterDistributionCoverageGapException extends RuntimeException {

    private final List<CostCenterDistributionPeriod> gaps;
    private final List<CostCenterDistributionPeriod> stretchCandidates;

    public CostCenterDistributionCoverageGapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            List<CostCenterDistributionPeriod> gaps,
            List<CostCenterDistributionPeriod> stretchCandidates
    ) {
        super("Cost center distributions must fully cover employee presence history for ruleSystemCode="
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

    public List<CostCenterDistributionPeriod> gaps() {
        return gaps;
    }

    public List<CostCenterDistributionPeriod> stretchCandidates() {
        return stretchCandidates;
    }
}
