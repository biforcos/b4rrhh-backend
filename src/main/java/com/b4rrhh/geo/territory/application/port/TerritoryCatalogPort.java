package com.b4rrhh.geo.territory.application.port;

import com.b4rrhh.geo.territory.domain.model.Country;
import com.b4rrhh.geo.territory.domain.model.Province;
import com.b4rrhh.geo.territory.domain.model.StreetType;

import java.util.List;

/**
 * Lo que del territorio es catálogo, leído desde una reglamentación (ADR-077): los países de
 * su capa de nivel 2, y las provincias —con su comunidad— y los tipos de vía de su capa
 * nacional.
 */
public interface TerritoryCatalogPort {

    boolean ruleSystemExists(String ruleSystemCode);

    /** Por nombre, en el idioma pedido. */
    List<Country> countries(String ruleSystemCode, String languageCode);

    /** Por código. */
    List<Province> provinces(String ruleSystemCode);

    /** Por código. */
    List<StreetType> streetTypes(String ruleSystemCode);
}
