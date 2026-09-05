package com.b4rrhh.employee.address.application.usecase;

import java.time.LocalDate;

/**
 * The user's correction of an address (ADR-057, decision 3). A {@code null}
 * {@code startDate} means the dates are not being corrected and both stay as
 * they are; otherwise the address takes {@code startDate} and {@code endDate}
 * as given, an absent {@code endDate} leaving it open.
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
