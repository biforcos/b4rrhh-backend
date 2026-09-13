package com.b4rrhh.payroll.domain.model;

import java.time.LocalDateTime;

/**
 * Una ejecucion de nomina y la cuenta de lo que hizo con cada candidato.
 *
 * <h2>Que cuenta cada contador (backend#85)</h2>
 *
 * Esto esta escrito aqui, y no en un issue, porque es aqui donde se lee antes de usarlos. Dos
 * lectores independientes dieron por hecho el mismo dia que {@code totalEligible} era el total
 * del universo —uno en prosa, la barra de progreso del panel viejo en codigo— y los dos se
 * equivocaron. Un contador que dos personas leen mal no esta mal leido: esta mal explicado.
 *
 * <ol>
 *   <li><b>{@code totalCandidates}</b> — el universo. Cuantas unidades de calculo salieron de
 *       la seleccion de objetivo, contadas <b>una vez, al principio</b>. Es el unico que no es
 *       un acumulador, y por tanto <b>el unico denominador legitimo de cualquier
 *       porcentaje</b>.</li>
 *   <li><b>{@code totalEligible}</b> — <b>no es un total: es un acumulador.</b> Sube una por
 *       cada unidad que pasa el filtro de elegibilidad, o sea que no existia ya un recibo
 *       inmutable suyo. Incluye a las que <b>despues</b> se saltan por falta de datos y a las
 *       que fallan: ser elegible es haber tenido derecho a que se intentara, no haber
 *       acabado bien. Solo llega a igualar a {@code totalCandidates} cuando no se salto
 *       ninguna, que es justo el caso en el que no se distingue de un total.</li>
 *   <li><b>{@code totalClaimed}</b> — unidades que consiguieron su reserva en
 *       {@code calculation_claim}, o sea que ninguna otra ejecucion las tenia cogidas.</li>
 *   <li><b>{@code totalSkippedNotEligible}</b> — saltadas porque <b>ya tenian recibo</b> y no
 *       estaba {@code NOT_VALID}. Esperable en un relanzamiento y <b>no pide nada de nadie</b>:
 *       es lo que impide recalcular lo que alguien pudo validar. Mensaje
 *       {@code UNIT_NOT_ELIGIBLE}. Estas unidades <b>no</b> estan en
 *       {@code totalEligible}.</li>
 *   <li><b>{@code totalSkippedAlreadyClaimed}</b> — saltadas porque otra ejecucion tenia la
 *       reserva. Mensaje {@code UNIT_ALREADY_CLAIMED}.</li>
 *   <li><b>{@code totalSkippedMissingInput}</b> — eran elegibles, se reservaron, y no se
 *       pudieron calcular porque faltaban datos. <b>Siempre pide que alguien mire.</b> Mensaje
 *       {@code UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT}. Estas unidades <b>si</b> estan en
 *       {@code totalEligible} y en {@code totalClaimed}.</li>
 *   <li><b>{@code totalCalculated}</b> — recibos calculados y no {@code NOT_VALID}.</li>
 *   <li><b>{@code totalNotValid}</b> — calculadas y marcadas {@code NOT_VALID} por el motor.
 *       Se calcularon: lo que dice este contador es el desenlace, no el intento.</li>
 *   <li><b>{@code totalErrors}</b> — la unidad revento con una excepcion no prevista. Mensaje
 *       {@code UNIT_CALCULATION_ERROR}.</li>
 * </ol>
 *
 * <p><b>La particion, que es lo que tiene que cuadrar:</b></p>
 *
 * <pre>
 * totalCandidates = totalSkippedNotEligible + totalSkippedAlreadyClaimed
 *                 + totalSkippedMissingInput + totalCalculated + totalNotValid + totalErrors
 * </pre>
 *
 * <p>Cuadra cuando la ejecucion recorrio todos sus candidatos. Una que murio a media
 * ejecucion —{@code RUN_ABANDONED_ON_RESTART}, {@code FAILED}— deja el resto sin contar en
 * ningun cajon, y la diferencia contra {@code totalCandidates} es exactamente lo que no llego
 * a mirar.</p>
 *
 * <p>{@code totalEligible} y {@code totalClaimed} <b>no entran en esa suma</b>: no son cajones,
 * son etapas por las que una unidad pasa antes de acabar en uno. Sumarlos con los demas es
 * contar dos veces, y ese fue el malentendido del backend#85.</p>
 *
 * <p>Antes del backend#85, {@code totalSkippedNotEligible} sumaba dos cosas de severidad
 * opuesta: «ya estaba hecho» y «faltan datos, que alguien mire». Su nombre nunca describio mal
 * lo suyo; describia mal lo que no era suyo. Quitandole el segundo camino pasa a ser verdad sin
 * migrar nada de lo que ya significaba.</p>
 *
 * <p>Los ocho {@code incrementTotal*} suman leyendo esta foto, construyendo otra y guardando la
 * fila entera: dos hilos que terminen dos unidades a la vez se pisan y ademas machacan los
 * otros contadores con los valores que cada uno leyo al empezar. Por eso el bucle de
 * unidades de {@code LaunchPayrollCalculationService} es secuencial a proposito (backend#83).</p>
 */
public record CalculationRun(
        Long id,
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        String calculationEngineCode,
        String calculationEngineVersion,
        LocalDateTime requestedAt,
        String requestedBy,
        String status,
        String targetSelectionJson,
        Integer totalCandidates,
        Integer totalEligible,
        Integer totalClaimed,
        Integer totalSkippedNotEligible,
        Integer totalSkippedAlreadyClaimed,
        Integer totalSkippedMissingInput,
        Integer totalCalculated,
        Integer totalNotValid,
        Integer totalErrors,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String summaryJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

        public CalculationRun withStatus(String newStatus) {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                newStatus,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun withStartedAt(LocalDateTime newStartedAt) {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                newStartedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun withFinishedExecution(String newStatus, LocalDateTime newFinishedAt, String newSummaryJson) {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                newStatus,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                newFinishedAt,
                                newSummaryJson,
                                createdAt,
                                updatedAt
                );
        }

        /**
         * Cierra una ejecucion que pudo no llegar a empezar nunca.
         *
         * <p>Una ejecucion puede morir encolada: se pidio, se quedo en REQUESTED y el
         * backend se reinicio, o la cola la rechazo. Hay que poder cerrarla igual, y el
         * esquema no admite una fecha de fin sin fecha de inicio
         * (chk_calculation_run_finished_requires_started, V55). Cuando no empezo, el
         * inicio que se le pone es el momento en que se pidio: lo mas cercano a la verdad
         * que existe en la fila. Que no arranco lo dice su mensaje, no esta fecha.
         */
        public CalculationRun withFinishedExecutionEvenIfNeverStarted(
                        String newStatus,
                        LocalDateTime newFinishedAt,
                        String newSummaryJson
        ) {
                return withStartedAt(startedAt == null ? requestedAt : startedAt)
                                .withFinishedExecution(newStatus, newFinishedAt, newSummaryJson);
        }

        public CalculationRun withTotalCandidates(int newTotalCandidates) {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                newTotalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun incrementTotalEligible() {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible + 1,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun incrementTotalClaimed() {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed + 1,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun incrementTotalSkippedNotEligible() {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible + 1,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun incrementTotalSkippedAlreadyClaimed() {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed + 1,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun incrementTotalSkippedMissingInput() {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput + 1,
                                totalCalculated,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun incrementTotalCalculated() {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated + 1,
                                totalNotValid,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun incrementTotalNotValid() {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid + 1,
                                totalErrors,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }

        public CalculationRun incrementTotalErrors() {
                return new CalculationRun(
                                id,
                                ruleSystemCode,
                                payrollPeriodCode,
                                payrollTypeCode,
                                calculationEngineCode,
                                calculationEngineVersion,
                                requestedAt,
                                requestedBy,
                                status,
                                targetSelectionJson,
                                totalCandidates,
                                totalEligible,
                                totalClaimed,
                                totalSkippedNotEligible,
                                totalSkippedAlreadyClaimed,
                                totalSkippedMissingInput,
                                totalCalculated,
                                totalNotValid,
                                totalErrors + 1,
                                startedAt,
                                finishedAt,
                                summaryJson,
                                createdAt,
                                updatedAt
                );
        }
}