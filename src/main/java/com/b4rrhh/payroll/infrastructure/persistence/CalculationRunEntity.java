package com.b4rrhh.payroll.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "calculation_run", schema = "payroll")
public class CalculationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_system_code", nullable = false, length = 5)
    private String ruleSystemCode;

    @Column(name = "payroll_period_code", nullable = false, length = 30)
    private String payrollPeriodCode;

    @Column(name = "payroll_type_code", nullable = false, length = 30)
    private String payrollTypeCode;

    @Column(name = "calculation_engine_code", nullable = false, length = 50)
    private String calculationEngineCode;

    @Column(name = "calculation_engine_version", nullable = false, length = 50)
    private String calculationEngineVersion;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_selection_json", nullable = false, columnDefinition = "json")
    private String targetSelectionJson;

    @Column(name = "total_candidates", nullable = false)
    private Integer totalCandidates;

    @Column(name = "total_eligible", nullable = false)
    private Integer totalEligible;

    @Column(name = "total_claimed", nullable = false)
    private Integer totalClaimed;

    @Column(name = "total_skipped_not_eligible", nullable = false)
    private Integer totalSkippedNotEligible;

    @Column(name = "total_skipped_already_claimed", nullable = false)
    private Integer totalSkippedAlreadyClaimed;

    @Column(name = "total_skipped_missing_input", nullable = false)
    private Integer totalSkippedMissingInput;

    @Column(name = "total_calculated", nullable = false)
    private Integer totalCalculated;

    @Column(name = "total_not_valid", nullable = false)
    private Integer totalNotValid;

    @Column(name = "total_errors", nullable = false)
    private Integer totalErrors;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json", columnDefinition = "json")
    private String summaryJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String ruleSystemCode) { this.ruleSystemCode = ruleSystemCode; }
    public String getPayrollPeriodCode() { return payrollPeriodCode; }
    public void setPayrollPeriodCode(String payrollPeriodCode) { this.payrollPeriodCode = payrollPeriodCode; }
    public String getPayrollTypeCode() { return payrollTypeCode; }
    public void setPayrollTypeCode(String payrollTypeCode) { this.payrollTypeCode = payrollTypeCode; }
    public String getCalculationEngineCode() { return calculationEngineCode; }
    public void setCalculationEngineCode(String calculationEngineCode) { this.calculationEngineCode = calculationEngineCode; }
    public String getCalculationEngineVersion() { return calculationEngineVersion; }
    public void setCalculationEngineVersion(String calculationEngineVersion) { this.calculationEngineVersion = calculationEngineVersion; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTargetSelectionJson() { return targetSelectionJson; }
    public void setTargetSelectionJson(String targetSelectionJson) { this.targetSelectionJson = targetSelectionJson; }
    public Integer getTotalCandidates() { return totalCandidates; }
    public void setTotalCandidates(Integer totalCandidates) { this.totalCandidates = totalCandidates; }
    public Integer getTotalEligible() { return totalEligible; }
    public void setTotalEligible(Integer totalEligible) { this.totalEligible = totalEligible; }
    public Integer getTotalClaimed() { return totalClaimed; }
    public void setTotalClaimed(Integer totalClaimed) { this.totalClaimed = totalClaimed; }
    public Integer getTotalSkippedNotEligible() { return totalSkippedNotEligible; }
    public void setTotalSkippedNotEligible(Integer totalSkippedNotEligible) { this.totalSkippedNotEligible = totalSkippedNotEligible; }
    public Integer getTotalSkippedAlreadyClaimed() { return totalSkippedAlreadyClaimed; }
    public void setTotalSkippedAlreadyClaimed(Integer totalSkippedAlreadyClaimed) { this.totalSkippedAlreadyClaimed = totalSkippedAlreadyClaimed; }

    public Integer getTotalSkippedMissingInput() { return totalSkippedMissingInput; }
    public void setTotalSkippedMissingInput(Integer totalSkippedMissingInput) { this.totalSkippedMissingInput = totalSkippedMissingInput; }
    public Integer getTotalCalculated() { return totalCalculated; }
    public void setTotalCalculated(Integer totalCalculated) { this.totalCalculated = totalCalculated; }
    public Integer getTotalNotValid() { return totalNotValid; }
    public void setTotalNotValid(Integer totalNotValid) { this.totalNotValid = totalNotValid; }
    public Integer getTotalErrors() { return totalErrors; }
    public void setTotalErrors(Integer totalErrors) { this.totalErrors = totalErrors; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getSummaryJson() { return summaryJson; }
    public void setSummaryJson(String summaryJson) { this.summaryJson = summaryJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}