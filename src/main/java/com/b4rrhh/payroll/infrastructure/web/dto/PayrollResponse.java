package com.b4rrhh.payroll.infrastructure.web.dto;

import com.b4rrhh.payroll.domain.model.PayrollStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param rulesChangedSinceCalculation si la reglamentación de su sistema de reglas se tocó después
 *        de calcularse este recibo ({@code backend#107}). El recibo sigue diciendo lo que el motor
 *        calculó —eso es el {@code ADR-062}—, pero deja de parecer que no ha pasado nada.
 *        <p><b>Sobre-avisa a propósito</b>: la fecha es del sistema de reglas entero, así que un
 *        cambio en un concepto que este empleado no usa lo levanta igual. Es la dirección segura,
 *        nunca dice fresco cuando está rancio, y por eso se pinta como «puede que ya no refleje las
 *        reglas actuales» y no como una afirmación.
 */
public record PayrollResponse(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber,
        PayrollStatus status,
        String statusReasonCode,
        LocalDateTime calculatedAt,
        String calculationEngineCode,
        String calculationEngineVersion,
        Long runId,
        List<PayrollWarningResponse> warnings,
        List<PayrollConceptResponse> concepts,
        List<PayrollContextSnapshotResponse> contextSnapshots,
        PayrollCompanyProfileResponse companyProfile,
        PayrollEmployeeProfileResponse employeeProfile,
        PayrollAgreementProfileResponse agreementProfile,
        String presenceStartDate,
        String presenceEndDate,
        String seniorityDate,
        String workCenterCode,
        String workCenterName,
        boolean rulesChangedSinceCalculation
) {
}