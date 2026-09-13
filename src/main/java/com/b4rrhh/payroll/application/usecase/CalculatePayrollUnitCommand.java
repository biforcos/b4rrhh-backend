package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;

import java.time.LocalDate;

/**
 * @param metamodel la reglamentación contra la que se calcula esta unidad. Va aquí, como un
 *                  dato más y al lado de las fechas del periodo, porque es exactamente eso:
 *                  un dato de entrada de la unidad, no algo que la unidad salga a buscar.
 *                  Quien lanza la ejecución lo carga una vez y se lo pasa a todas sus
 *                  unidades, y así todas calculan con las mismas reglas por construcción
 *                  (backend#87).
 */
public record CalculatePayrollUnitCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber,
        LocalDate periodStart,
        LocalDate periodEnd,
        String calculationEngineCode,
        String calculationEngineVersion,
        Long runId,
        RuleSystemMetamodel metamodel
) {
}