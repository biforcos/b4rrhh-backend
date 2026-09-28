package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.PayrollStatus;

/** Los filtros de la búsqueda de recibos y la página que se pide ({@code b4rrhh/frontend#93}). */
public record SearchPayrollsQuery(
        String ruleSystemCode,
        String payrollPeriodCode,
        String employeeNumber,
        PayrollStatus status,
        int page,
        int size
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;
}
