package com.b4rrhh.employee.address.application.usecase;

import java.time.LocalDate;

/**
 * The user's correction of an address (ADR-057, decision 3). The address
 * takes {@code startDate} and {@code endDate} as given, an absent
 * {@code endDate} leaving it open. {@code startDate} is required: leaving the
 * address where it starts means sending the same date again, not leaving it
 * out (backend#69).
 */
public record UpdateAddressCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer addressNumber,
        String street,
        String city,
        String countryCode,
        String postalCode,
        String regionCode,
        LocalDate startDate,
        LocalDate endDate
) {
}
