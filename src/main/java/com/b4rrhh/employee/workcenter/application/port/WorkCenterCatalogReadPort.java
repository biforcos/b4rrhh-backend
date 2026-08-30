package com.b4rrhh.employee.workcenter.application.port;

import java.time.LocalDate;
import java.util.Optional;

public interface WorkCenterCatalogReadPort {

    default Optional<String> findWorkCenterName(String ruleSystemCode, String workCenterCode) {
        return findWorkCenterName(ruleSystemCode, workCenterCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findWorkCenterName(String ruleSystemCode, String workCenterCode, String languageCode);

    Optional<String> findWorkCenterCompanyCode(String ruleSystemCode, String workCenterCode, LocalDate referenceDate);

    default Optional<String> findCompanyName(String ruleSystemCode, String companyCode) {
        return findCompanyName(ruleSystemCode, companyCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findCompanyName(String ruleSystemCode, String companyCode, String languageCode);
}
