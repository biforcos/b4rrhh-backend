package com.b4rrhh.payroll_engine.metamodel.domain.exception;

/**
 * Una vigencia de la reglamentacion que no cubre periodos naturales enteros.
 *
 * El mensaje nombra el campo real y dice que se esperaba, porque un 400 mudo aqui deja al
 * que lo recibe sin saber si el problema es el dia, el mes o el orden de las dos fechas.
 */
public class ValidityWindowDoesNotCoverWholePeriodsException extends RuntimeException {

    public ValidityWindowDoesNotCoverWholePeriodsException(String message) {
        super(message);
    }
}
