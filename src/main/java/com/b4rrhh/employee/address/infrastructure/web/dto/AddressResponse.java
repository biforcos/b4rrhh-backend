package com.b4rrhh.employee.address.infrastructure.web.dto;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressResponse;

import java.time.LocalDate;

public record AddressResponse(
        Integer addressNumber,
        String addressTypeCode,
        String addressTypeName,
        PostalAddressResponse address,
        LocalDate startDate,
        LocalDate endDate
) {
}
