package com.b4rrhh.rulesystem.catalogoption.application.query;

import java.time.LocalDate;

/**
 * @param languageCode idioma del literal (BCP 47 corto, {@code es-ES}), o {@code null} para el
 *                     literal base. Lo resuelve la capa web desde {@code Accept-Language}; aquí
 *                     sólo viaja hasta el puerto, que ya sabía traducir desde el backend#24 y al
 *                     que nadie se lo pasaba (b4rrhh/backend#143).
 */
public record GetDirectCatalogOptionsQuery(
        String ruleSystemCode,
        String ruleEntityTypeCode,
        LocalDate referenceDate,
        String q,
        String languageCode
) {

    public GetDirectCatalogOptionsQuery(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            LocalDate referenceDate,
            String q
    ) {
        this(ruleSystemCode, ruleEntityTypeCode, referenceDate, q, null);
    }
}
