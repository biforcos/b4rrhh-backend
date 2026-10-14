package com.b4rrhh.geo.territory.application.port;

import com.b4rrhh.geo.territory.domain.model.Country;
import com.b4rrhh.geo.territory.domain.model.Province;
import com.b4rrhh.geo.territory.domain.model.StreetType;

import java.util.List;
import java.util.Optional;

/**
 * Lo que del territorio es catálogo (ADR-077): los países que ve una reglamentación, de su capa de
 * nivel 2, y las provincias —con su comunidad— y los tipos de vía de un país, de su capa nacional.
 *
 * <p>El país se resuelve a su capa nacional por {@link #nationalRuleSystemOf}: la reglamentación que
 * lo tiene como suyo, y de ella su capa de nivel 3 (backend#169). Así lee el formulario y así valida
 * el backend#155, por la misma puerta.</p>
 */
public interface TerritoryCatalogPort {

    boolean ruleSystemExists(String ruleSystemCode);

    /** Por nombre, en el idioma pedido. */
    List<Country> countries(String ruleSystemCode, String languageCode);

    /**
     * La reglamentación cuyo país es éste, que es la que monta su capa nacional; vacío si ninguna lo
     * tiene como suyo (ITA). Si hubiera varias, la primera por código: hoy hay una por país.
     */
    Optional<String> nationalRuleSystemOf(String countryCode);

    /** Si el código es una entidad de ese tipo en la capa nacional del país, vigente o no. */
    boolean inNationalCatalog(String countryCode, String ruleEntityTypeCode, String code);

    /** Por código. */
    List<Province> provinces(String ruleSystemCode);

    /** Por código. */
    List<StreetType> streetTypes(String ruleSystemCode);
}
