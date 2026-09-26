package com.b4rrhh.payroll.retro.domain.model;

/**
 * A que total de <b>este</b> mes suma una linea de atraso ({@code backend#133}).
 *
 * <p>Y la clasificacion no sale de una lista escrita a mano: sale del <b>grafo</b>. Un concepto suma a
 * los devengos atrasados si alimenta al {@code 970}, a las deducciones si alimenta al {@code 980} y a la
 * aportacion de la empresa si alimenta al {@code 725}. Es la regla de este motor —lo que interviene en un
 * calculo se ve en el grafo (V146)— y tiene un premio: un concepto nuevo que alimente al {@code 970}
 * lleva sus atrasos al {@code 970} sin que nadie anada una linea aqui.
 */
public enum RetroConceptBucket {

    /** Alimenta al total de devengos. Es mas dinero para el empleado. */
    EARNING,

    /** Alimenta al total de deducciones. Es menos dinero para el empleado. */
    EMPLOYEE_DEDUCTION,

    /** Alimenta al total de aportacion de la empresa. No pasa por el liquido. */
    EMPLOYER_CONTRIBUTION,

    /**
     * No suma a ningun total de este mes.
     *
     * <p>Son las <b>bases</b>, y esa es la otra mitad de la decision del 05/10: una base se atribuye a
     * SU mes. La linea de base con origen agosto existe en el recibo de septiembre para que se pueda leer
     * y para la liquidacion complementaria a la Seguridad Social, y <b>no</b> suma a las bases de
     * septiembre, que son las de septiembre.
     */
    NEITHER
}
