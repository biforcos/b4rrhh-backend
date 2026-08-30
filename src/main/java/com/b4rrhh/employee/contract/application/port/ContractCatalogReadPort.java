package com.b4rrhh.employee.contract.application.port;

import java.util.Optional;

public interface ContractCatalogReadPort {

    default Optional<String> findContractTypeName(String ruleSystemCode, String contractCode) {
        return findContractTypeName(ruleSystemCode, contractCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findContractTypeName(String ruleSystemCode, String contractCode, String languageCode);

    default Optional<String> findContractSubtypeName(String ruleSystemCode, String contractSubtypeCode) {
        return findContractSubtypeName(ruleSystemCode, contractSubtypeCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findContractSubtypeName(String ruleSystemCode, String contractSubtypeCode, String languageCode);
}
