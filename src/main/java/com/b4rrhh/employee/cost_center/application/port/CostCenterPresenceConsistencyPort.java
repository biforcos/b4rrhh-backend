package com.b4rrhh.employee.cost_center.application.port;

import java.util.List;

/**
 * How the cost center series reads the employee's presence (ADR-057). It only
 * reads it: whether the resulting series stays inside it and covers it is the
 * component's judgement, never a check this vertical makes on its own.
 */
public interface CostCenterPresenceConsistencyPort {

    /** The presence periods that frame the cost center series (ADR-057), oldest first. */
    List<PresencePeriod> findPresencePeriodsByEmployeeIdOrderByStartDate(Long employeeId);
}
