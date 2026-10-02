package com.b4rrhh.geo.territory.domain.model;

/**
 * Lo que dice un código postal: la provincia que nombran sus dos primeras cifras, el
 * municipio que sugiere la fuente y, cuando no hay provincia, por qué. Una respuesta, no un
 * error: que las cifras no sean una provincia es algo que hay que poder decir.
 */
public record PostalCodeCheck(
        String postalCode,
        String countryCode,
        Province province,
        Municipality suggestedMunicipality,
        String message
) {
}
