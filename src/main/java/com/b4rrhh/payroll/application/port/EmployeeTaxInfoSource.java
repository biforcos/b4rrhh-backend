package com.b4rrhh.payroll.application.port;

/**
 * De dónde salió la situación fiscal con la que se calculó un recibo.
 *
 * <p>Existe porque el snapshot de ADR-059 es «lo que el motor tuvo delante», y hasta el
 * {@code backend#92} una situación supuesta se guardaba con exactamente la misma forma que una
 * declarada: mirando el recibo no había manera de saber cuál de las dos era. Una ausencia
 * invisible es peor que una ausencia visible.
 */
public enum EmployeeTaxInfoSource {

    /** El empleado tiene una declaración vigente a la fecha de referencia, y es la que se usó. */
    DECLARED,

    /**
     * No hay declaración a esa fecha y se calculó con la situación por omisión.
     *
     * <p>No es un error: es lo que hace cualquier nómina real con quien no ha presentado su
     * modelo 145. Lo que este valor añade es que quede escrito que se hizo así.
     */
    DEFAULT_NO_DECLARATION
}
