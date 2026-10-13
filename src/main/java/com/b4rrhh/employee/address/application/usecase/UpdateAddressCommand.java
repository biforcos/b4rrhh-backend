package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.geo.address.domain.model.PostalAddressInput;

import java.time.LocalDate;

/**
 * The user's correction of an address (ADR-057, decision 3). The address
 * takes {@code startDate} and {@code endDate} as given, an absent
 * {@code endDate} leaving it open. {@code startDate} is required: leaving the
 * address where it starts means sending the same date again, not leaving it
 * out (backend#69). {@code address} is the whole corrected address, not a
 * patch: a field left out is a field left empty (backend#155).
 */
public record UpdateAddressCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer addressNumber,
        PostalAddressInput address,
        LocalDate startDate,
        LocalDate endDate
) {
}
