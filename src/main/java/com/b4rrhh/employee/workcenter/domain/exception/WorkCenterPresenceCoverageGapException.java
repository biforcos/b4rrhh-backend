package com.b4rrhh.employee.workcenter.domain.exception;

import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;

import java.util.List;

/**
 * The resulting series would leave a stretch of the employee's presence
 * without a work center assignment (ADR-057): the coverage of this series is
 * mandatory. It names the gaps and the neighbouring assignments the user
 * could stretch to cover them.
 */
public class WorkCenterPresenceCoverageGapException extends RuntimeException {

    private final List<WorkCenterPeriod> gaps;
    private final List<WorkCenterOccurrence> stretchCandidates;

    public WorkCenterPresenceCoverageGapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, List.of(), List.of());
    }

    public WorkCenterPresenceCoverageGapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            List<WorkCenterPeriod> gaps,
            List<WorkCenterOccurrence> stretchCandidates
    ) {
        super("Work center history must fully cover employee presence history for ruleSystemCode="
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

    public List<WorkCenterPeriod> gaps() {
        return gaps;
    }

    public List<WorkCenterOccurrence> stretchCandidates() {
        return stretchCandidates;
    }
}
