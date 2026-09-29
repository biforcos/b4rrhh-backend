package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.lifecycle.application.model.IdentifierOwner;

import java.util.Optional;

/**
 * De quién es ya un documento en un sistema de reglas, preguntado antes del alta
 * (b4rrhh/backend#149). Es cortesía para la pantalla: la garantía sigue siendo el 409 del alta.
 */
public interface FindIdentifierOwnerUseCase {

    Optional<IdentifierOwner> findOwner(String ruleSystemCode, String identifierTypeCode, String identifierValue);
}
