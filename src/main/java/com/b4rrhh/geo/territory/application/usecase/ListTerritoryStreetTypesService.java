package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.application.port.TerritoryCatalogPort;
import com.b4rrhh.geo.territory.domain.model.StreetType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTerritoryStreetTypesService implements ListTerritoryStreetTypesUseCase {

    private final TerritoryCatalogPort catalog;

    public ListTerritoryStreetTypesService(TerritoryCatalogPort catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<StreetType> list(String ruleSystemCode) {
        return catalog.streetTypes(TerritoryRuleSystem.require(catalog, ruleSystemCode));
    }
}
