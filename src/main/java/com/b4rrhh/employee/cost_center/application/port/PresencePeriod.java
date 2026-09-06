package com.b4rrhh.employee.cost_center.application.port;

import java.time.LocalDate;

/** A presence period of the employee, as the cost center series reads it to frame its invariants (ADR-057). */
public record PresencePeriod(
        LocalDate startDate,
        LocalDate endDate
) {
}
