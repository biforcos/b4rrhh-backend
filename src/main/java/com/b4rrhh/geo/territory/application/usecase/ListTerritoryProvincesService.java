package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.application.port.TerritoryCatalogPort;
import com.b4rrhh.geo.territory.domain.model.Province;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTerritoryProvincesService implements ListTerritoryProvincesUseCase {

    private final TerritoryCatalogPort catalog;

    public ListTerritoryProvincesService(TerritoryCatalogPort catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Province> list(String ruleSystemCode) {
        return catalog.provinces(TerritoryRuleSystem.require(catalog, ruleSystemCode));
    }
}
