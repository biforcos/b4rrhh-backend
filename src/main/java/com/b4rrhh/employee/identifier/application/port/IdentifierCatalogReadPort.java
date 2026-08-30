package com.b4rrhh.employee.identifier.application.port;

import java.util.Optional;

public interface IdentifierCatalogReadPort {

    default Optional<String> findIdentifierTypeName(String ruleSystemCode, String identifierTypeCode) {
        return findIdentifierTypeName(ruleSystemCode, identifierTypeCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findIdentifierTypeName(String ruleSystemCode, String identifierTypeCode, String languageCode);
}
