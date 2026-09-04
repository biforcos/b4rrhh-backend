package com.b4rrhh.employee.working_time.domain.exception;

import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;

import java.util.List;

/**
 * The resulting series would leave a stretch of the employee's presence
 * without a working time (ADR-057): the coverage of this series is
 * mandatory. It names the gaps and the neighbouring occurrences the user
 * could stretch to cover them.
 */
public class WorkingTimeCoverageGapException extends RuntimeException {

    private final List<WorkingTimePeriod> gaps;
    private final List<WorkingTimeOccurrence> stretchCandidates;

    public WorkingTimeCoverageGapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            List<WorkingTimePeriod> gaps,
            List<WorkingTimeOccurrence> stretchCandidates
    ) {
        super("Working time series would leave the presence uncovered for ruleSystemCode="
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

    public List<WorkingTimePeriod> gaps() {
        return gaps;
    }

    public List<WorkingTimeOccurrence> stretchCandidates() {
        return stretchCandidates;
    }
}
