package com.b4rrhh.payroll_engine.execution.domain.model;

import java.math.BigDecimal;

/**
 * Un tramo de dias de la prestacion por incapacidad temporal ({@code backend#129}).
 *
 * <p>Los tramos son una <b>tabla</b> y no un {@code if} en Java, por lo mismo que el tipo de desempleo
 * (ADR-072) y los topes de cada ejercicio (V153): son cifras de una norma, cambian sin que cambie el
 * programa, y cada una tiene que poder llevar su cita al lado.
 *
 * @param dayFrom primer dia de baja del tramo, contado desde el inicio de la ausencia
 * @param dayTo ultimo dia del tramo, o {@code null} si el tramo no termina
 * @param percentage el porcentaje de la base reguladora que se paga en el tramo
 */
public record ItPrestacionTramo(
        int dayFrom,
        Integer dayTo,
        BigDecimal percentage
) {

    /**
     * Cuantos dias de un intervalo de dias de baja caen en este tramo.
     *
     * <p>Los dos extremos van incluidos, y el resultado no es nunca negativo: un tramo que no toca el
     * intervalo aporta cero dias y no un numero al reves.
     *
     * @param firstDay primer dia de baja del intervalo, contado desde el inicio de la ausencia (1 es
     *        el primer dia de la baja)
     * @param lastDay ultimo dia de baja del intervalo, incluido
     */
    public long daysWithin(long firstDay, long lastDay) {
        long desde = Math.max(firstDay, dayFrom);
        long hasta = dayTo == null ? lastDay : Math.min(lastDay, dayTo);
        return Math.max(0, hasta - desde + 1);
    }
}
