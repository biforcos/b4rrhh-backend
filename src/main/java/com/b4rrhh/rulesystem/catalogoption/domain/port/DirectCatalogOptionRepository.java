package com.b4rrhh.rulesystem.catalogoption.domain.port;

import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;

import java.time.LocalDate;
import java.util.List;

public interface DirectCatalogOptionRepository {

    default List<DirectCatalogOption> findDirectOptions(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            LocalDate referenceDate,
            String qLike
    ) {
        return findDirectOptions(ruleSystemCode, ruleEntityTypeCode, referenceDate, qLike, null);
    }

    /**
     * Las opciones con el literal en el idioma pedido (BCP 47 corto) cuando hay traducción, o
     * el base si no; {@code null} pide el literal base (ADR-052 §3, backend#24). El filtro
     * {@code qLike} y el orden siguen siendo sobre el literal base.
     */
    List<DirectCatalogOption> findDirectOptions(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            LocalDate referenceDate,
            String qLike,
            String languageCode
    );
}
