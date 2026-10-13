package com.b4rrhh.geo.address.infrastructure.web.dto;

import com.b4rrhh.geo.address.domain.model.PostalAddressInput;

/**
 * Una dirección en una petición: la misma en el domicilio del empleado, la empresa y el centro
 * (backend#155). Es el único DTO de dirección de entrada; {@code AddressStructureIsTheSameInTheThreeTablesTest}
 * vigila que no salga otro.
 */
public record PostalAddressRequest(
        String streetTypeCode,
        String streetName,
        String streetNumber,
        String numberQualifier,
        String block,
        String stair,
        String floor,
        String door,
        String locality,
        String municipalityCode,
        String subdivisionCode,
        String postalCode,
        String countryCode
) {

    public static PostalAddressInput toInput(PostalAddressRequest request) {
        if (request == null) {
            return null;
        }
        return new PostalAddressInput(request.streetTypeCode, request.streetName, request.streetNumber,
                request.numberQualifier, request.block, request.stair, request.floor, request.door,
                request.locality, request.municipalityCode, request.subdivisionCode, request.postalCode,
                request.countryCode);
    }
}
