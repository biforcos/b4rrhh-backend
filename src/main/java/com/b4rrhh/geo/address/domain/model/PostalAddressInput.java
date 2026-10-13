package com.b4rrhh.geo.address.domain.model;

/**
 * Una dirección tal y como llega, antes de validarla. La valida
 * {@code PostalAddressValidator}, que es quien decide si queda normalizada.
 */
public record PostalAddressInput(
        String streetTypeCode,
        String streetName,
        String streetNumber,
        String numberQualifier,
        String block,
        String portal,
        String stair,
        String floor,
        String door,
        String locality,
        String municipalityCode,
        String subdivisionCode,
        String postalCode,
        String countryCode
) {
}
