package com.b4rrhh.employee.lifecycle.domain.exception;

/**
 * El documento del alta está mal escrito (backend#171): el mensaje es el del validador, con el
 * motivo en palabras. Antes salía como «valor de catálogo inválido» y nadie sabía qué catálogo era
 * (frontend#133).
 */
public class HireEmployeeIdentifierInvalidException extends RuntimeException {

    public HireEmployeeIdentifierInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
