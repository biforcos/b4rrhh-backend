package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.application.port.TerritoryCatalogPort;
import com.b4rrhh.geo.territory.domain.model.Country;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTerritoryCountriesService implements ListTerritoryCountriesUseCase {

    private final TerritoryCatalogPort catalog;

    public ListTerritoryCountriesService(TerritoryCatalogPort catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Country> list(String ruleSystemCode, String languageCode) {
        return catalog.countries(TerritoryRuleSystem.require(catalog, ruleSystemCode), languageCode);
    }
}
