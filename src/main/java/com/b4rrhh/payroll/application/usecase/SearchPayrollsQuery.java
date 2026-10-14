package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.PayrollStatus;

/**
 * Los filtros de la búsqueda de recibos y la página que se pide ({@code b4rrhh/frontend#93}).
 *
 * @param employeeNumber la matrícula exacta: el salto desde la ficha, que sabe a quién busca
 * @param text           lo que se escribe en la lista: parte de la matrícula o del nombre
 *                       ({@code b4rrhh/frontend#141}); si viene, manda sobre la matrícula exacta
 */
public record SearchPayrollsQuery(
        String ruleSystemCode,
        String payrollPeriodCode,
        String employeeNumber,
        PayrollStatus status,
        int page,
        int size,
        String text
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    /** Sin texto: la búsqueda de siempre. */
    public SearchPayrollsQuery(String ruleSystemCode, String payrollPeriodCode, String employeeNumber,
                               PayrollStatus status, int page, int size) {
        this(ruleSystemCode, payrollPeriodCode, employeeNumber, status, page, size, null);
    }
}
