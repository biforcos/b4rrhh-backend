package com.b4rrhh.employee.labor_classification.application.port;

import java.util.Optional;

public interface LaborClassificationCatalogReadPort {

    default Optional<String> findAgreementName(String ruleSystemCode, String agreementCode) {
        return findAgreementName(ruleSystemCode, agreementCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findAgreementName(String ruleSystemCode, String agreementCode, String languageCode);

    default Optional<String> findAgreementCategoryName(String ruleSystemCode, String agreementCategoryCode) {
        return findAgreementCategoryName(ruleSystemCode, agreementCategoryCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findAgreementCategoryName(String ruleSystemCode, String agreementCategoryCode, String languageCode);

    Optional<String> findGrupoCotizacionCode(String ruleSystemCode, String agreementCategoryCode);
}