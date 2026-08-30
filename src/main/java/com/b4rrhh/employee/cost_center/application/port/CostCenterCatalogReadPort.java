package com.b4rrhh.employee.cost_center.application.port;

import java.util.Optional;

/**
 * Read port for looking up catalog names for enriching cost center read models.
 */
public interface CostCenterCatalogReadPort {

    Optional<String> findCostCenterName(String ruleSystemCode, String costCenterCode);
}
