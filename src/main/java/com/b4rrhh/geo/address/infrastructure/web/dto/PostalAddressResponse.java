package com.b4rrhh.geo.address.infrastructure.web.dto;

import com.b4rrhh.geo.address.domain.model.PostalAddress;

/**
 * Una dirección en una respuesta: la misma en los tres sitios (backend#155). {@code normalized} a
 * {@code false} es «sin normalizar», y la ficha lo enseña; {@code municipalityName} es el nombre que
 * el maestro da a {@code municipalityCode}.
 */
public record PostalAddressResponse(
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
        String municipalityName,
        String subdivisionCode,
        String postalCode,
        String countryCode,
        boolean normalized
) {

    /**
     * La de una empresa o un centro, que sale siempre: vacía y sin normalizar si no tiene. Era así
     * antes del backend#155 y la pantalla cuenta con ello.
     */
    public static PostalAddressResponse ofOrEmpty(PostalAddress address) {
        return address == null
                ? new PostalAddressResponse(null, null, null, null, null, null, null, null, null, null, null, null, null, null, false)
                : of(address);
    }

    public static PostalAddressResponse of(PostalAddress address) {
        if (address == null) {
            return null;
        }
        return new PostalAddressResponse(address.streetTypeCode(), address.streetName(), address.streetNumber(),
                address.numberQualifier(), address.block(), address.stair(), address.floor(), address.door(),
                address.locality(), address.municipalityCode(), address.municipalityName(),
                address.subdivisionCode(), address.postalCode(), address.countryCode(), address.normalized());
    }
}
