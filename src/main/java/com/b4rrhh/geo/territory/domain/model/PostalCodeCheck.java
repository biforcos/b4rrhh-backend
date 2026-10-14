package com.b4rrhh.geo.territory.domain.model;

/**
 * Lo que dice un código postal: la provincia que nombran sus dos primeras cifras, el
 * municipio que sugiere la fuente, si está en el maestro y, cuando no hay provincia, por qué.
 * Una respuesta, no un error: que las cifras no sean una provincia es algo que hay que poder
 * decir.
 *
 * <p>{@code known} es lo que decide la marca {@code CP_DESCONOCIDO} (backend#155): sin él, un CP
 * que no existe y uno que existe sin municipio sugerido contestaban igual (backend#170).</p>
 */
public record PostalCodeCheck(
        String postalCode,
        String countryCode,
        Province province,
        Municipality suggestedMunicipality,
        boolean known,
        String message
) {
}
