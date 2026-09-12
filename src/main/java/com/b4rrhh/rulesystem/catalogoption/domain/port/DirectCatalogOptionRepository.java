package com.b4rrhh.rulesystem.catalogoption.domain.port;

import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;

import java.util.List;

public interface DirectCatalogOptionRepository {

    default List<DirectCatalogOption> findDirectOptions(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String qLike
    ) {
        return findDirectOptions(ruleSystemCode, ruleEntityTypeCode, qLike, null);
    }

    /**
     * Las opciones con el literal en el idioma pedido (BCP 47 corto) cuando hay traducción, o
     * el base si no; {@code null} pide el literal base (ADR-052 §3, backend#24). El filtro
     * {@code qLike} y el orden siguen siendo sobre el literal base.
     *
     * <p>No lleva fecha a propósito (backend#32): el catálogo se devuelve entero y la
     * vigencia la fecha quien pregunta, marcando cada opción. Volver a meterla aquí sería
     * volver a filtrar.
     */
    List<DirectCatalogOption> findDirectOptions(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String qLike,
            String languageCode
    );
}
