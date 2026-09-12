package com.b4rrhh.payroll.domain.model;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import com.b4rrhh.payroll.domain.exception.PayrollTypeInvalidException;

import java.time.LocalDateTime;
import java.util.List;

public class Payroll {

    private final Long id;
    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String employeeNumber;
    private final String payrollPeriodCode;
    private final String payrollTypeCode;
    private final Integer presenceNumber;
    private final PayrollStatus status;
    private final String statusReasonCode;
    private final LocalDateTime calculatedAt;
    private final String calculationEngineCode;
    private final String calculationEngineVersion;
    // La ejecucion que produjo este recibo. No es mutable: si se recalcula, el recibo es otro y
    // lleva el suyo (ADR-059, backend#62). Null significa que no hubo ejecucion registrada, que es
    // el caso de los dos caminos que todavia crean recibo a mano: el endpoint temporal de calculo
    // y el recalculo puntual. Las sobrecargas de conveniencia de create y rehydrate delegan con
    // null por eso mismo; el camino de lanzamiento pasa siempre por la sobrecarga completa.
    private final Long runId;
    private final List<PayrollWarning> warnings;
    private final List<PayrollConcept> concepts;
    private final List<PayrollContextSnapshot> contextSnapshots;
    private final List<PayrollSegment> segments;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Payroll(
            Long id,
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
            List<PayrollWarning> warnings,
            List<PayrollConcept> concepts,
            List<PayrollContextSnapshot> contextSnapshots,
            List<PayrollSegment> segments,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.ruleSystemCode = requireCode(ruleSystemCode, "ruleSystemCode", 5);
        this.employeeTypeCode = requireCode(employeeTypeCode, "employeeTypeCode", 30);
        this.employeeNumber = requireText(employeeNumber, "employeeNumber", 15);
        this.payrollPeriodCode = requireCode(payrollPeriodCode, "payrollPeriodCode", 30);
        String normalizedPayrollTypeCode = requireCode(payrollTypeCode, "payrollTypeCode", 30);
        if (!PayrollTypeCodes.isValid(normalizedPayrollTypeCode)) {
            throw new PayrollTypeInvalidException(normalizedPayrollTypeCode);
        }
        this.payrollTypeCode = normalizedPayrollTypeCode;
        this.presenceNumber = requirePositive(presenceNumber, "presenceNumber");
        this.status = requireStatus(status);
        this.statusReasonCode = normalizeReason(statusReasonCode);
        this.calculatedAt = requireCalculatedAt(calculatedAt);
        this.calculationEngineCode = requireCode(calculationEngineCode, "calculationEngineCode", 50);
        this.calculationEngineVersion = requireText(calculationEngineVersion, "calculationEngineVersion", 50);
        this.runId = runId;
        this.warnings = copyWarnings(warnings);
        this.concepts = copyConcepts(concepts);
        this.contextSnapshots = copySnapshots(contextSnapshots);
        this.segments = List.copyOf(segments != null ? segments : List.of());
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payroll create(
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
            List<PayrollWarning> warnings,
            List<PayrollConcept> concepts,
            List<PayrollContextSnapshot> contextSnapshots
    ) {
        return create(ruleSystemCode, employeeTypeCode, employeeNumber, payrollPeriodCode,
                payrollTypeCode, presenceNumber, status, statusReasonCode, calculatedAt,
                calculationEngineCode, calculationEngineVersion, null, warnings, concepts,
                contextSnapshots, List.of());
    }

    public static Payroll create(
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
            List<PayrollWarning> warnings,
            List<PayrollConcept> concepts,
            List<PayrollContextSnapshot> contextSnapshots,
            List<PayrollSegment> segments
    ) {
        return new Payroll(
                null,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber,
                status,
                statusReasonCode,
                calculatedAt,
                calculationEngineCode,
                calculationEngineVersion,
                runId,
                warnings,
                concepts,
                contextSnapshots,
                segments,
                null,
                null
        );
    }

    public static Payroll create(
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
            List<PayrollConcept> concepts,
            List<PayrollContextSnapshot> contextSnapshots
    ) {
        return create(ruleSystemCode, employeeTypeCode, employeeNumber, payrollPeriodCode,
                payrollTypeCode, presenceNumber, status, statusReasonCode, calculatedAt,
                calculationEngineCode, calculationEngineVersion, null, List.of(), concepts,
                contextSnapshots, List.of());
    }

    public static Payroll rehydrate(
            Long id,
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
            List<PayrollWarning> warnings,
            List<PayrollConcept> concepts,
            List<PayrollContextSnapshot> contextSnapshots,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return rehydrate(id, ruleSystemCode, employeeTypeCode, employeeNumber, payrollPeriodCode,
                payrollTypeCode, presenceNumber, status, statusReasonCode, calculatedAt,
                calculationEngineCode, calculationEngineVersion, null, warnings, concepts,
                contextSnapshots, List.of(), createdAt, updatedAt);
    }

    public static Payroll rehydrate(
            Long id,
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
            List<PayrollWarning> warnings,
            List<PayrollConcept> concepts,
            List<PayrollContextSnapshot> contextSnapshots,
            List<PayrollSegment> segments,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Payroll(
                id,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber,
                status,
                statusReasonCode,
                calculatedAt,
                calculationEngineCode,
                calculationEngineVersion,
                runId,
                warnings,
                concepts,
                contextSnapshots,
                segments,
                createdAt,
                updatedAt
        );
    }

    public static Payroll rehydrate(
            Long id,
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
            List<PayrollConcept> concepts,
            List<PayrollContextSnapshot> contextSnapshots,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return rehydrate(id, ruleSystemCode, employeeTypeCode, employeeNumber, payrollPeriodCode,
                payrollTypeCode, presenceNumber, status, statusReasonCode, calculatedAt,
                calculationEngineCode, calculationEngineVersion, null, List.of(), concepts,
                contextSnapshots, List.of(), createdAt, updatedAt);
    }

    public Payroll invalidate(String statusReasonCode) {
        if (status != PayrollStatus.CALCULATED && status != PayrollStatus.EXPLICIT_VALIDATED) {
            throw new PayrollInvalidStateTransitionException(status, "invalidate");
        }
        return withStatus(PayrollStatus.NOT_VALID, requireCode(statusReasonCode, "statusReasonCode", 50));
    }

    public Payroll validateExplicitly() {
        if (status != PayrollStatus.CALCULATED) {
            throw new PayrollInvalidStateTransitionException(status, "validate");
        }
        return withStatus(PayrollStatus.EXPLICIT_VALIDATED, statusReasonCode);
    }

    public Payroll finalizePayroll() {
        if (status != PayrollStatus.CALCULATED && status != PayrollStatus.EXPLICIT_VALIDATED) {
            throw new PayrollInvalidStateTransitionException(status, "finalize");
        }
        return withStatus(PayrollStatus.DEFINITIVE, statusReasonCode);
    }

    private Payroll withStatus(PayrollStatus nextStatus, String nextStatusReasonCode) {
        return new Payroll(
                id,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber,
                nextStatus,
                nextStatusReasonCode,
                calculatedAt,
                calculationEngineCode,
                calculationEngineVersion,
                runId,
                warnings,
                concepts,
                contextSnapshots,
                segments,
                createdAt,
                updatedAt == null ? null : LocalDateTime.now()
        );
    }

    private static PayrollStatus requireStatus(PayrollStatus value) {
        if (value == null) {
            throw new InvalidPayrollArgumentException("status is required");
        }
        return value;
    }

    private static LocalDateTime requireCalculatedAt(LocalDateTime value) {
        if (value == null) {
            throw new InvalidPayrollArgumentException("calculatedAt is required");
        }
        return value;
    }

    private static Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new InvalidPayrollArgumentException(fieldName + " must be a positive integer");
        }
        return value;
    }

    private static String requireCode(String value, String fieldName, int maxLength) {
        String normalized = requireText(value, fieldName, maxLength);
        return normalized.toUpperCase();
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidPayrollArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidPayrollArgumentException(fieldName + " exceeds max length " + maxLength);
        }
        return normalized;
    }

    private static String normalizeReason(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requireCode(value, "statusReasonCode", 50);
    }

    private static List<PayrollConcept> copyConcepts(List<PayrollConcept> concepts) {
        if (concepts == null) {
            throw new InvalidPayrollArgumentException("concepts is required");
        }
        return List.copyOf(concepts);
    }

    private static List<PayrollWarning> copyWarnings(List<PayrollWarning> warnings) {
        if (warnings == null) {
            throw new InvalidPayrollArgumentException("warnings is required");
        }
        return List.copyOf(warnings);
    }

    private static List<PayrollContextSnapshot> copySnapshots(List<PayrollContextSnapshot> contextSnapshots) {
        if (contextSnapshots == null) {
            throw new InvalidPayrollArgumentException("contextSnapshots is required");
        }
        return List.copyOf(contextSnapshots);
    }

    public boolean canBeRecalculated() {
        return status.canBeRecalculated();
    }

    public Long getId() {
        return id;
    }

    public String getRuleSystemCode() {
        return ruleSystemCode;
    }

    public String getEmployeeTypeCode() {
        return employeeTypeCode;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getPayrollPeriodCode() {
        return payrollPeriodCode;
    }

    public String getPayrollTypeCode() {
        return payrollTypeCode;
    }

    public Integer getPresenceNumber() {
        return presenceNumber;
    }

    public PayrollStatus getStatus() {
        return status;
    }

    public String getStatusReasonCode() {
        return statusReasonCode;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public String getCalculationEngineCode() {
        return calculationEngineCode;
    }

    public String getCalculationEngineVersion() {
        return calculationEngineVersion;
    }

    public Long getRunId() {
        return runId;
    }

    public List<PayrollWarning> getWarnings() {
        return warnings;
    }

    public List<PayrollConcept> getConcepts() {
        return concepts;
    }

    public List<PayrollContextSnapshot> getContextSnapshots() {
        return contextSnapshots;
    }

    public List<PayrollSegment> getSegments() {
        return segments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}