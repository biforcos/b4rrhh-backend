package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.application.port.TerritoryCatalogPort;
import com.b4rrhh.geo.territory.domain.exception.TerritoryRuleSystemNotFoundException;

final class TerritoryRuleSystem {

    private TerritoryRuleSystem() {
    }

    static String require(TerritoryCatalogPort catalog, String ruleSystemCode) {
        String code = ruleSystemCode == null ? "" : ruleSystemCode.strip().toUpperCase();
        if (code.isEmpty() || !catalog.ruleSystemExists(code)) {
            throw new TerritoryRuleSystemNotFoundException(ruleSystemCode);
        }
        return code;
    }
}
