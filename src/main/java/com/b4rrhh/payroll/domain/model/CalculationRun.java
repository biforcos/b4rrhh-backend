package com.b4rrhh.payroll.domain.model;

import java.time.LocalDateTime;

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