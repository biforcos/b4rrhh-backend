package com.b4rrhh.employee.address.application.port;

import java.util.Optional;

public interface AddressCatalogReadPort {

    default Optional<String> findAddressTypeName(String ruleSystemCode, String addressTypeCode) {
        return findAddressTypeName(ruleSystemCode, addressTypeCode, null);
    }

    /**
     * El literal en el idioma pedido (BCP 47 corto, {@code es-ES}) o el base si no hay
     * traducción; {@code null} pide el literal base. La sobrecarga sin idioma se conserva
     * tal cual: es el vocabulario del vertical (ADR-052 §3, backend#24).
     */
    Optional<String> findAddressTypeName(String ruleSystemCode, String addressTypeCode, String languageCode);
}
