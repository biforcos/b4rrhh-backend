package com.b4rrhh.geo.address.domain.model;

/**
 * Una dirección, la misma en los tres sitios que la guardan: el domicilio del empleado, el de la
 * empresa y el del centro (backend#155, ADR-078). La estructura es la del domicilio de la AEAT y la
 * TGSS; cada tabla la guarda en sus columnas, con los mismos nombres y tipos.
 *
 * <p>Los códigos apuntan al territorio pero no son claves ajenas: {@code streetTypeCode} y
 * {@code subdivisionCode} son entidades {@code STREET_TYPE} y {@code PROVINCE} de la capa ESP,
 * {@code municipalityCode} un municipio de {@code geo}, y {@code countryCode} un {@code COUNTRY} de
 * INT. Una dirección que no cumple la validación se guarda igual, con el motivo en
 * {@code normalizationIssue}: «sin normalizar». Un estado, no un silencio.</p>
 *
 * <p>{@code municipalityName} no se escribe: es lo que dice el maestro de {@code municipalityCode}
 * al leer, y {@code null} si el código no está.</p>
 */
public record PostalAddress(
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
        String countryCode,
        PostalAddressIssue normalizationIssue,
        String municipalityName
) {

    /** Normalizada es no tener motivo para no estarlo. */
    public boolean normalized() {
        return normalizationIssue == null;
    }

    /** La línea de la vía, para un recibo o un listado: «CL Mayor, 3, 3º D». {@code null} si no hay vía. */
    public String streetLine() {
        StringBuilder line = new StringBuilder();
        append(line, " ", streetTypeCode);
        append(line, " ", streetName);
        if (streetNumber != null) {
            line.append(line.isEmpty() ? "" : ", ").append(streetNumber);
            append(line, " ", numberQualifier);
        }
        if (block != null) {
            append(line, ", ", "bl. " + block);
        }
        if (portal != null) {
            append(line, ", ", "portal " + portal);
        }
        if (stair != null) {
            append(line, ", ", "esc. " + stair);
        }
        if (floor != null) {
            append(line, ", ", floor + "º");
        }
        append(line, " ", door);
        return line.isEmpty() ? null : line.toString();
    }

    /** La población, para un recibo o un listado: el municipio, con la pedanía delante si la hay. */
    public String townLine() {
        if (locality != null && municipalityName != null && !locality.equalsIgnoreCase(municipalityName)) {
            return locality + ", " + municipalityName;
        }
        return municipalityName != null ? municipalityName : locality;
    }

    private static void append(StringBuilder line, String separator, String part) {
        if (part == null) {
            return;
        }
        if (!line.isEmpty()) {
            line.append(separator);
        }
        line.append(part);
    }
}
