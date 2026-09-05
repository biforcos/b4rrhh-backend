package com.b4rrhh.employee.contract.infrastructure.rest.dto;

import java.time.LocalDate;

/**
 * {@code startDate} is the corrected start, omitted to keep the one in the
 * path; {@code endDate} is the corrected end, omitted for a contract that
 * stays open (ADR-057).
 */
public record UpdateContractRequest(
        LocalDate startDate,
        LocalDate endDate,
        String contractCode,
        String contractSubtypeCode
) {
}
