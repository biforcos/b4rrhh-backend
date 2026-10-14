package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.application.port.TerritoryCatalogPort;
import com.b4rrhh.geo.territory.domain.model.CountryTerritory;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Lo que un país tiene de territorio (backend#169). Las listas de un país sin territorio vienen
 * vacías, y vacío no dice por qué: esto sí. Fuera de los países con capa nacional, una dirección es
 * país, código postal y texto (ADR-078).
 */
@Service
public class DescribeTerritoryOfCountryService implements DescribeTerritoryOfCountryUseCase {

    private final TerritoryCatalogPort catalog;

    public DescribeTerritoryOfCountryService(TerritoryCatalogPort catalog) {
        this.catalog = catalog;
    }

    @Override
    public CountryTerritory describe(String countryCode) {
        String country = countryCode == null ? "" : countryCode.strip().toUpperCase(Locale.ROOT);
        boolean hasTerritory = catalog.nationalRuleSystemOf(country).isPresent();
        return new CountryTerritory(country, hasTerritory, hasTerritory ? null
                : country + " no tiene territorio cargado: ninguna reglamentación lo tiene como suyo, así que "
                + "no hay provincias ni tipos de vía de allí. Una dirección de " + country
                + " es país, código postal y texto.");
    }
}
