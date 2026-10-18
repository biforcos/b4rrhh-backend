package com.b4rrhh.employee.photo.domain;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Donde vive la foto de un empleado en el almacén: {@code photos/ESP/INTERNAL/EMP000025/<uuid>.jpg}.
 *
 * <p>Es lo que guarda {@code employee.photo_url} desde {@code backend#177}: la clave y nada más. Antes
 * guardaba la URL entera del almacén tal como la veía el backend que la recibió, y eso no servía a
 * ningún navegador fuera de esa máquina.
 *
 * <p>El UUID no es decoración: cada foto nueva tiene clave nueva, así que su enlace cambia y ninguna
 * caché sirve la de antes. Por eso hace además de versión ({@link #version()}).
 */
public record EmployeePhotoKey(String value) {

    private static final Pattern FORMA =
            Pattern.compile("photos/[^/]+/[^/]+/[^/]+/([0-9a-f-]{36})\\.jpg");

    public EmployeePhotoKey {
        if (value == null || !FORMA.matcher(value).matches()) {
            throw new IllegalArgumentException("No es una clave de foto: " + value);
        }
    }

    public static EmployeePhotoKey newFor(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        return new EmployeePhotoKey("photos/" + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber
                + "/" + UUID.randomUUID() + ".jpg");
    }

    /** El UUID de la clave: cambia con cada foto, y es lo que lleva el enlace como {@code ?v=}. */
    public String version() {
        Matcher m = FORMA.matcher(value);
        m.matches();
        return m.group(1);
    }
}
