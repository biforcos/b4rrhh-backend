package com.b4rrhh.payroll.application.usecase;

/**
 * @param requestedBy quien pide el recalculo. Va aqui porque un recalculo abre su propia ejecucion
 *                    (#99) y una ejecucion dice quien la pidio, igual que la del lanzamiento. Puede
 *                    ser nulo: los escenarios y los tests recalculan sin nadie autenticado detras.
 */
public record RecalculatePayrollCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber,
        String requestedBy
) {}
