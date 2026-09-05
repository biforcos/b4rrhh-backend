package com.b4rrhh.employee.contract.domain.model;

import java.time.LocalDate;

/**
 * A stretch of dates in the contract series as a plan or an error names it:
 * an occurrence (a contract is identified by the day it starts, so its dates
 * are all a plan needs to name it), a gap the series would leave inside the
 * presence, or the dates two contracts would share. An open end date means
 * "onwards".
 */
public record ContractPeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public ContractPeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
