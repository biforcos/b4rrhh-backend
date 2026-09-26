package com.b4rrhh.employee.shared.application.port;

/**
 * Como se nombra cada vertical cuando avisa de una escritura con fecha ({@code backend#130}).
 *
 * <p>Estan aqui y no dispersos porque la ficha del empleado y la checklist del ciclo agrupan por
 * este codigo: dos verticales que se nombren distinto en dos sitios salen como tres grupos.
 *
 * <p>El codigo es el de la vertical y no el de la tabla: la tabla va aparte en el propio aviso,
 * porque una vertical puede tener mas de una y lo que se ensena es la vertical.
 */
public final class DatedWriteSources {

    public static final String ABSENCE               = "ABSENCE";
    public static final String CONTRACT              = "CONTRACT";
    public static final String COST_CENTER           = "COST_CENTER";
    public static final String EXTRA_PAYMENT_REGIME  = "EXTRA_PAYMENT_REGIME";
    public static final String LABOR_CLASSIFICATION  = "LABOR_CLASSIFICATION";
    public static final String PAYROLL_INPUT         = "PAYROLL_INPUT";
    public static final String WORK_CENTER           = "WORK_CENTER";
    public static final String WORKING_TIME          = "WORKING_TIME";

    private DatedWriteSources() {
    }
}
