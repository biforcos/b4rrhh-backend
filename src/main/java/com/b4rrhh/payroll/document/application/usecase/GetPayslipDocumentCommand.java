package com.b4rrhh.payroll.document.application.usecase;

/** La clave de negocio del recibo cuyo documento se pide ({@code backend#112}). */
public record GetPayslipDocumentCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber
) {
}
