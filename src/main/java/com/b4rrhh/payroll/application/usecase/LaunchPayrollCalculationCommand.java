package com.b4rrhh.payroll.application.usecase;

public record LaunchPayrollCalculationCommand(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        String calculationEngineCode,
        String calculationEngineVersion,
        PayrollLaunchTargetSelection targetSelection,
        // Quien pide la ejecucion. Lo rellena la capa web con el sujeto del token; los
        // lanzamientos en proceso (tests y escenarios) lo dejan nulo porque no hay nadie
        // detras. No se valida contra el modelo de usuarios: es traza, no identidad.
        String requestedBy
) {
}