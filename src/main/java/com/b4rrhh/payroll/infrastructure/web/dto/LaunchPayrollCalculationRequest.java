package com.b4rrhh.payroll.infrastructure.web.dto;

/**
 * @param retroLimitPeriodCode hasta que mes atras permite recalcular este lanzamiento
 *        ({@code backend#132}). <b>Obligatorio</b> desde la API: un formulario propone un valor y quien
 *        lanza lo confirma o lo cambia. Si falta, la corrida no hace retro y lo dice en sus mensajes
 * @param retroFloorPeriodCode el suelo para todos, si lo hay: todo empleado recalcula desde aqui aunque
 *        no tenga ninguna marca. Opcional, y nulo es lo normal
 */
public record LaunchPayrollCalculationRequest(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        String calculationEngineCode,
        String calculationEngineVersion,
        PayrollLaunchTargetSelectionRequest targetSelection,
        String retroLimitPeriodCode,
        String retroFloorPeriodCode
) {
}