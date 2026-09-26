package com.b4rrhh.payroll.retro.domain.model;

/**
 * Quien sabe a que total suma cada concepto ({@code backend#133}).
 *
 * <p>Es una funcion y no un mapa porque quien la resuelve es el <b>grafo de la ejecucion</b>, que la
 * unidad ya tiene delante: mirar quien alimenta al {@code 970}, al {@code 980} y al {@code 725}. Pasarla
 * como dependencia deja el calculo del delta sin saber nada del metamodelo.
 */
public interface RetroBucketing {

    RetroConceptBucket bucketOf(String conceptCode);
}
