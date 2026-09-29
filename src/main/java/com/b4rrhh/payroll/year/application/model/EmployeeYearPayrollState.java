package com.b4rrhh.payroll.year.application.model;

/**
 * En qué está un mes para este empleado. Cerrado es que todos sus recibos del mes son
 * DEFINITIVE, que es lo que el ADR-074 y las marcas de retro llaman «entregado»; abierto, que
 * alguno no lo es. Un mes sin recibo no tiene estado.
 */
public enum EmployeeYearPayrollState {
    OPEN,
    CLOSED
}
