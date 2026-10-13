package com.b4rrhh.employee.address.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressRequest;

import java.time.LocalDate;

public record CreateAddressRequest(
        String addressTypeCode,
        PostalAddressRequest address,
        LocalDate startDate,
        LocalDate endDate
) {
}
