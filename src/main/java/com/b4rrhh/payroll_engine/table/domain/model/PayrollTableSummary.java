package com.b4rrhh.payroll_engine.table.domain.model;

import java.util.List;

/**
 * Una tabla de verdad de un sistema de reglas.
 *
 * "De verdad" quiere decir que tiene filas, o que hay una vinculacion que
 * apunta a ella, o las dos cosas. Ninguna de las dos fuentes sola contesta la
 * pregunta (backend#95):
 *
 *   - una tabla con filas y sin vinculaciones existe y no la lee nadie;
 *   - una tabla vinculada y sin filas se lee y esta vacia.
 *
 * Los dos son estados distintos y los dos hay que poder verlos, que es para lo
 * que el recuento viaja al lado de las vinculaciones.
 *
 * Y el recuento son dos: una tabla a la que le desactivaron todas las filas
 * sigue teniendo filas y ya no alimenta nada, que no es lo mismo que una vacia.
 */
public record PayrollTableSummary(
        String ruleSystemCode,
        String tableCode,
        long rowCount,
        long activeRowCount,
        List<PayrollTableBinding> bindings
) {
}
