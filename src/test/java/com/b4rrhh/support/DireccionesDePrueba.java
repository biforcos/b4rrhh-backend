package com.b4rrhh.support;

import com.b4rrhh.geo.address.application.service.PostalAddressValidator;
import com.b4rrhh.geo.address.domain.exception.PostalAddressRejectedException;
import com.b4rrhh.geo.address.domain.model.PostalAddress;
import com.b4rrhh.geo.address.domain.model.PostalAddressInput;

import java.time.LocalDate;

/**
 * Direcciones para los tests unitarios que no van de la dirección (backend#155).
 *
 * <p>Los que sí van de ella —qué queda normalizado y qué se rechaza— prueban
 * {@link PostalAddressValidator} contra el esquema real, con su catálogo y su maestro
 * {@code geo}. Aquí sólo hace falta una dirección que pase: la vía en {@code streetName}, la
 * población en {@code locality} y el país, como las escribían los tests cuando la dirección era
 * {@code street}, {@code city} y {@code countryCode}.</p>
 */
public final class DireccionesDePrueba {

    private DireccionesDePrueba() {
    }

    public static PostalAddressInput input(String street, String city, String countryCode, String postalCode) {
        return new PostalAddressInput(null, street, null, null, null, null, null, null, city, null, null,
                postalCode, countryCode);
    }

    public static PostalAddress address(String street, String city, String countryCode, String postalCode) {
        return new PostalAddress(null, street, null, null, null, null, null, null, city, null, null,
                postalCode, countryCode == null ? null : countryCode.trim().toUpperCase(), false, null);
    }

    /**
     * Una validación que no mira el catálogo: devuelve lo que llega, con los códigos en mayúsculas
     * y sin normalizar. Rechaza sólo la dirección sin país, que es lo que rechaza la de verdad sin
     * necesitar el catálogo.
     */
    public static PostalAddressValidator validacionQueNoMiraElCatalogo() {
        return new PostalAddressValidator(null, null) {
            @Override
            public PostalAddress validate(String ruleSystemCode, PostalAddressInput input, LocalDate date) {
                if (input == null || input.countryCode() == null || input.countryCode().isBlank()) {
                    throw new PostalAddressRejectedException("countryCode is required: an address without a country is not stored");
                }
                return new PostalAddress(upper(input.streetTypeCode()), trim(input.streetName()), trim(input.streetNumber()),
                        trim(input.numberQualifier()), trim(input.block()), trim(input.stair()), trim(input.floor()),
                        trim(input.door()), trim(input.locality()), upper(input.municipalityCode()),
                        upper(input.subdivisionCode()), trim(input.postalCode()), upper(input.countryCode()), false, null);
            }
        };
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String upper(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }
}
