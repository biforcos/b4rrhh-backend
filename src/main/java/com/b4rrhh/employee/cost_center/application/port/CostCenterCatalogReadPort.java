package com.b4rrhh.employee.cost_center.application.port;

import java.util.Optional;

/**
 * Read port for looking up catalog names for enriching cost center read models.
 */
public interface CostCenterCatalogReadPort {

    default Optional<String> findCostCenterName(String ruleSystemCode, String costCenterCode) {
        return findCostCenterName(ruleSystemCode, costCenterCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findCostCenterName(String ruleSystemCode, String costCenterCode, String languageCode);
}
