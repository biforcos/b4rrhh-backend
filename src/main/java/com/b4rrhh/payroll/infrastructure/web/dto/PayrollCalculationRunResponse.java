package com.b4rrhh.payroll.infrastructure.web.dto;

import java.time.LocalDateTime;

/**
 * La ejecucion como la ve la API.
 *
 * <h2>Los cinco campos de la retro ({@code backend#132})</h2>
 *
 * <p>{@code retroLimitPeriodCode} y {@code retroFloorPeriodCode} dicen <b>con que se calculo</b>, que es
 * lo que el recibo y la checklist del ciclo tienen que poder contar.
 *
 * <p>{@code totalRetroUnits} es el universo de la retro: unidades <b>empleado x mes</b>, contadas una
 * vez y al principio como {@code totalCandidates}. Es una terna aparte y <b>no se suma a los nueve
 * contadores del recibo</b>, porque una unidad de retro no acaba en ninguno de esos cajones —no escribe
 * recibo, escribe calculo vigente— y meterla habria roto su particion sin que nada avisara. Su propia
 * particion es {@code totalRetroUnits = totalRetroRecalculated + totalRetroNotRecalculated}.
 *
 * <p>Y el trabajo de verdad de la corrida, que es lo que la pantalla tiene que ensenar, es
 * {@code totalCandidates + totalRetroUnits}: un lanzamiento con suelo para todos que dijera «873
 * candidatos» mientras calcula siete mil meses pareceria colgado.
 *
 * <p>La documentacion de cada campo vive en el contrato OpenAPI, que es la fuente. Aqui no hay
 * comentarios <b>dentro</b> de la lista de componentes a proposito: el candado
 * {@code TheTwoContractsNeverDivergeInSilenceTest} lee esta lista con una expresion regular para
 * cruzarla con el contrato, y un bloque de javadoc entre componentes le hace ver campos que no existen.
 */
public record PayrollCalculationRunResponse(
        Long runId,
        String status,
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        String calculationEngineCode,
        String calculationEngineVersion,
        Integer totalCandidates,
        Integer totalEligible,
        Integer totalClaimed,
        Integer totalSkippedNotEligible,
        Integer totalSkippedAlreadyClaimed,
        Integer totalSkippedMissingInput,
        Integer totalCalculated,
        Integer totalNotValid,
        Integer totalErrors,
        String retroLimitPeriodCode,
        String retroFloorPeriodCode,
        Integer totalRetroUnits,
        Integer totalRetroRecalculated,
        Integer totalRetroNotRecalculated,
        LocalDateTime requestedAt,
        String requestedBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
