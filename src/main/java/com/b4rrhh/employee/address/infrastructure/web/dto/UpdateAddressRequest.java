package com.b4rrhh.employee.address.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressRequest;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * A correction of an address (ADR-057, decision 3): the whole corrected
 * address and its dates, with {@code endDate} absent for an address that stays
 * open. {@code startDate} is required (backend#69).
 */
public record UpdateAddressRequest(
        PostalAddressRequest address,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
) {
}
