package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;

import java.time.LocalDate;
import java.util.List;

/**
 * @param metamodel la reglamentación contra la que se calcula esta unidad. Va aquí, como un
 *                  dato más y al lado de las fechas del periodo, porque es exactamente eso:
 *                  un dato de entrada de la unidad, no algo que la unidad salga a buscar.
 *                  Quien lanza la ejecución lo carga una vez y se lo pasa a todas sus
 *                  unidades, y así todas calculan con las mismas reglas por construcción
 *                  (backend#87).
 * @param retroFromPeriodCode el mes más antiguo del tramo de retro de esta corrida, o {@code null} si
 *                  esta unidad no paga atrasos ({@code backend#133}). Lo dice quien lanza y no lo adivina
 *                  la unidad: el tramo está acotado por el límite del lanzamiento, y una unidad que
 *                  buscara «todos los vigentes que hay» se saltaría ese límite el día que exista un
 *                  vigente de una corrida anterior más generosa
 * @param retroToPeriodCode {@code P-1}, o {@code null}
 * @param retroMarksOutsideLimit los periodos de las marcas de retroactividad que el límite del
 *                  lanzamiento deja fuera ({@code backend#132}). Cada uno sale como aviso en el recibo
 *                  de este periodo: <b>no se paga y no se calla</b>. Viene dado y la unidad no lo
 *                  calcula, porque quién decide el límite es quien lanza y la unidad no tiene que saber
 *                  que existe una retro
 * @param retroMonthsNotRecalculated los meses del tramo que no se pudieron recalcular
 *                  ({@code backend#136}). Tampoco se pagan, y tampoco se callan: cada uno sale como aviso
 *                  en el recibo de este periodo
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
        RuleSystemMetamodel metamodel,
        List<String> retroMarksOutsideLimit,
        String retroFromPeriodCode,
        String retroToPeriodCode,
        List<String> retroMonthsNotRecalculated
) {

    public CalculatePayrollUnitCommand {
        // Nunca nula, para que quien la lea no tenga que preguntarse si nulo es «ninguna» o «no me lo
        // han dicho».
        retroMarksOutsideLimit = retroMarksOutsideLimit == null
                ? List.of() : List.copyOf(retroMarksOutsideLimit);
        retroMonthsNotRecalculated = retroMonthsNotRecalculated == null
                ? List.of() : List.copyOf(retroMonthsNotRecalculated);
    }

    /** El mandato de siempre: sin marcas fuera de límite que avisar. */
    public CalculatePayrollUnitCommand(
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
        this(ruleSystemCode, employeeTypeCode, employeeNumber, payrollPeriodCode, payrollTypeCode,
                presenceNumber, periodStart, periodEnd, calculationEngineCode, calculationEngineVersion,
                runId, metamodel, List.of(), null, null, List.of());
    }
}
