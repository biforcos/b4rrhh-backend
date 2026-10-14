package com.b4rrhh.rulesystem.catalogoption.infrastructure.web.dto;

import java.time.LocalDate;

/**
 * Una opción de un desplegable.
 *
 * @param commonLabel la etiqueta en palabras de una cita reglamentaria (backend#173): va delante y la
 *                    cita detrás; null en todo lo demás
 */
public record DirectCatalogOptionResponse(
        String code,
        String name,
        boolean active,
        LocalDate startDate,
        LocalDate endDate,
        String commonLabel
) {
}
