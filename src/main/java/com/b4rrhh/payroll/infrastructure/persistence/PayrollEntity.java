package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.PayrollStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "payroll", schema = "payroll")
public class PayrollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_system_code", nullable = false, length = 5)
    private String ruleSystemCode;

    @Column(name = "employee_type_code", nullable = false, length = 30)
    private String employeeTypeCode;

    @Column(name = "employee_number", nullable = false, length = 15)
    private String employeeNumber;

    @Column(name = "payroll_period_code", nullable = false, length = 30)
    private String payrollPeriodCode;

    @Column(name = "payroll_type_code", nullable = false, length = 30)
    private String payrollTypeCode;

    @Column(name = "presence_number", nullable = false)
    private Integer presenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayrollStatus status;

    @Column(name = "status_reason_code", length = 50)
    private String statusReasonCode;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "calculation_engine_code", nullable = false, length = 50)
    private String calculationEngineCode;

    @Column(name = "calculation_engine_version", nullable = false, length = 50)
    private String calculationEngineVersion;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PayrollConceptEntity> concepts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PayrollContextSnapshotEntity> contextSnapshots = new LinkedHashSet<>();

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollWarningEntity> warnings = new ArrayList<>();

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PayrollSegmentEntity> segments = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void replaceConcepts(List<PayrollConceptEntity> items) {
        concepts.clear();
        if (items == null) {
            return;
        }
        items.forEach(this::addConcept);
    }

    public void replaceContextSnapshots(List<PayrollContextSnapshotEntity> items) {
        contextSnapshots.clear();
        if (items == null) {
            return;
        }
        items.forEach(this::addContextSnapshot);
    }

    public void replaceWarnings(List<PayrollWarningEntity> items) {
        warnings.clear();
        if (items == null) {
            return;
        }
        items.forEach(this::addWarning);
    }

    public void replaceSegments(java.util.Collection<PayrollSegmentEntity> items) {
        segments.clear();
        if (items == null) {
            return;
        }
        items.forEach(this::addSegment);
    }

    private void addConcept(PayrollConceptEntity concept) {
        concept.setPayroll(this);
        concepts.add(concept);
    }

    private void addContextSnapshot(PayrollContextSnapshotEntity snapshot) {
        snapshot.setPayroll(this);
        contextSnapshots.add(snapshot);
    }

    private void addWarning(PayrollWarningEntity warning) {
        warning.setPayroll(this);
        warnings.add(warning);
    }

    private void addSegment(PayrollSegmentEntity segment) {
        segment.setPayroll(this);
        segments.add(segment);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String ruleSystemCode) { this.ruleSystemCode = ruleSystemCode; }
    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public void setEmployeeTypeCode(String employeeTypeCode) { this.employeeTypeCode = employeeTypeCode; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getPayrollPeriodCode() { return payrollPeriodCode; }
    public void setPayrollPeriodCode(String payrollPeriodCode) { this.payrollPeriodCode = payrollPeriodCode; }
    public String getPayrollTypeCode() { return payrollTypeCode; }
    public void setPayrollTypeCode(String payrollTypeCode) { this.payrollTypeCode = payrollTypeCode; }
    public Integer getPresenceNumber() { return presenceNumber; }
    public void setPresenceNumber(Integer presenceNumber) { this.presenceNumber = presenceNumber; }
    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus status) { this.status = status; }
    public String getStatusReasonCode() { return statusReasonCode; }
    public void setStatusReasonCode(String statusReasonCode) { this.statusReasonCode = statusReasonCode; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public String getCalculationEngineCode() { return calculationEngineCode; }
    public void setCalculationEngineCode(String calculationEngineCode) { this.calculationEngineCode = calculationEngineCode; }
    public String getCalculationEngineVersion() { return calculationEngineVersion; }
    public void setCalculationEngineVersion(String calculationEngineVersion) { this.calculationEngineVersion = calculationEngineVersion; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Set<PayrollConceptEntity> getConcepts() { return concepts; }
    public Set<PayrollContextSnapshotEntity> getContextSnapshots() { return contextSnapshots; }
    public List<PayrollWarningEntity> getWarnings() { return warnings; }
    public Set<PayrollSegmentEntity> getSegments() { return segments; }
}