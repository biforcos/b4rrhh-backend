package com.b4rrhh.payroll_engine.table.domain.model;

/**
 * Cuantas filas tiene un codigo de tabla, y cuantas de ellas siguen activas.
 */
public record PayrollTableRowCount(String tableCode, long rowCount, long activeRowCount) {
}
