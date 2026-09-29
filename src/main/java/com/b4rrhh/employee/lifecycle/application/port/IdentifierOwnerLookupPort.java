package com.b4rrhh.employee.lifecycle.application.port;

import com.b4rrhh.employee.lifecycle.application.model.IdentifierOwner;

import java.util.Optional;

/**
 * Quién tiene ya un documento en un sistema de reglas (b4rrhh/backend#141). El valor se compara
 * sin mayúsculas ni espacios: « 12345678z » y «12345678Z» son el mismo DNI.
 */
public interface IdentifierOwnerLookupPort {

    Optional<IdentifierOwner> findOwner(String ruleSystemCode, String identifierTypeCode, String identifierValue);
}
