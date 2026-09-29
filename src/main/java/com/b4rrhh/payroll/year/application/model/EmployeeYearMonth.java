package com.b4rrhh.payroll.year.application.model;

/**
 * Un mes del año: su estado de nómina y cuántas cosas pasan en él. Las entradas y las marcas se
 * cuentan, no se traen: se ven al bajar a ese mes.
 *
 * @param payrollState nulo si el empleado no tiene recibo ese mes
 */
public record EmployeeYearMonth(
        String payrollPeriodCode,
        EmployeeYearPayrollState payrollState,
        int payrollInputCount,
        int payrollInputConceptCount,
        int activeRetroMarkCount,
        int consumedRetroMarkCount
) {
}
