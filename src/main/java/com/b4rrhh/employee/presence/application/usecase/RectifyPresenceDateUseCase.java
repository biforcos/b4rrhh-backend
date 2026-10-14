package com.b4rrhh.employee.presence.application.usecase;

/**
 * La fecha de alta o de cese de una presencia, rectificada con todo lo que empezó (o acabó) con ella
 * (backend#172). Un verbo propio y no una edición de campo: es el alta que se tecleó mal.
 */
public interface RectifyPresenceDateUseCase {

    RectifyPresenceDateResult rectify(RectifyPresenceDateCommand command);
}
