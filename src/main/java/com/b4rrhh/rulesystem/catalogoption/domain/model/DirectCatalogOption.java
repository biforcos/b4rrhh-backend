package com.b4rrhh.rulesystem.catalogoption.domain.model;

import java.time.LocalDate;

/**
 * Una opción de un desplegable.
 *
 * @param name        el literal del código en el idioma pedido: la traducción si la hay, o el base
 * @param commonLabel la etiqueta en palabras de una cita reglamentaria (backend#173), que acompaña a
 *                    {@code name} sin sustituirlo; {@code null} en todo lo demás
 */
public record DirectCatalogOption(
        String code,
        String name,
        boolean active,
        LocalDate startDate,
        LocalDate endDate,
        String commonLabel
) {

    /** Sin etiqueta común: todo lo que no es una cita que la necesite. */
    public DirectCatalogOption(String code, String name, boolean active, LocalDate startDate, LocalDate endDate) {
        this(code, name, active, startDate, endDate, null);
    }

    public DirectCatalogOption {
        code = normalizeRequired("code", code);
        name = normalizeRequired("name", name);
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
    }

    public boolean isEffectiveOn(LocalDate date) {
        return !startDate.isAfter(date)
                && (endDate == null || !endDate.isBefore(date));
    }

    public DirectCatalogOption withActive(boolean newActive) {
        return new DirectCatalogOption(code, name, newActive, startDate, endDate, commonLabel);
    }

    private static String normalizeRequired(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }
}
