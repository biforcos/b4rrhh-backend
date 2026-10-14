package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.lifecycle.application.model.IdentifierCheck;

/**
 * Si un documento está bien escrito y de quién es ya en un sistema de reglas, preguntado antes del
 * alta (b4rrhh/backend#149, backend#171). Es cortesía para la pantalla: la garantía sigue siendo el
 * alta, que se niega con el mismo motivo.
 */
public interface FindIdentifierOwnerUseCase {

    IdentifierCheck check(
            String ruleSystemCode,
            String identifierTypeCode,
            String identifierValue,
            String issuingCountryCode
    );
}
