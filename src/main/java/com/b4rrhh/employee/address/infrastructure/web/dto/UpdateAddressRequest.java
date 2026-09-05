package com.b4rrhh.employee.address.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * A correction of an address (ADR-057, decision 3). Omit {@code startDate} to
 * leave the dates as they are; give it to correct them, with {@code endDate}
 * absent for an address that stays open.
 */
public record UpdateAddressRequest(
        String street,
        String city,
        String countryCode,
        String postalCode,
        String regionCode,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
) {
}
