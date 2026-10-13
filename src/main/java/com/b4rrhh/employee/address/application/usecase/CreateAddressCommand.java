package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.geo.address.domain.model.PostalAddressInput;

import java.time.LocalDate;

public record CreateAddressCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String addressTypeCode,
        PostalAddressInput address,
        LocalDate startDate,
        LocalDate endDate
) {
}
