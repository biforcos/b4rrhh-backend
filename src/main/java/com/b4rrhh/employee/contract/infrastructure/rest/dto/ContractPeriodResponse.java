package com.b4rrhh.employee.contract.infrastructure.rest.dto;

import java.time.LocalDate;

/**
 * A stretch of dates named by a plan or an error: a contract (identified by
 * its start date), a gap, or an overlap. Open end date means onwards.
 */
public record ContractPeriodResponse(
        LocalDate startDate,
        LocalDate endDate
) {
}
