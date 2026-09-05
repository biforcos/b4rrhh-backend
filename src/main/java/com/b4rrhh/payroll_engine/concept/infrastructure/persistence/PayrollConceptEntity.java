package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity(name = "PayrollEngineConceptEntity")
@Table(name = "payroll_concept", schema = "payroll_engine")
public class PayrollConceptEntity {

    @Id
    private Long objectId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "object_id")
    private PayrollObjectEntity payrollObject;

    @Column(name = "concept_mnemonic", nullable = false, length = 50)
    private String conceptMnemonic;

    @Column(name = "calculation_type", nullable = false, length = 30)
    private String calculationType;

    @Column(name = "functional_nature", nullable = false, length = 30)
    private String functionalNature;

    @Column(name = "payslip_order_code", length = 30)
    private String payslipOrderCode;

    @Column(name = "execution_scope", nullable = false, length = 30)
    private String executionScope;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

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

    public Long getObjectId() { return objectId; }
    public void setObjectId(Long objectId) { this.objectId = objectId; }

    public PayrollObjectEntity getPayrollObject() { return payrollObject; }
    public void setPayrollObject(PayrollObjectEntity payrollObject) { this.payrollObject = payrollObject; }

    public String getConceptMnemonic() { return conceptMnemonic; }
    public void setConceptMnemonic(String conceptMnemonic) { this.conceptMnemonic = conceptMnemonic; }

    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }

    public String getFunctionalNature() { return functionalNature; }
    public void setFunctionalNature(String functionalNature) { this.functionalNature = functionalNature; }

    public String getPayslipOrderCode() { return payslipOrderCode; }
    public void setPayslipOrderCode(String payslipOrderCode) { this.payslipOrderCode = payslipOrderCode; }

    public String getExecutionScope() { return executionScope; }
    public void setExecutionScope(String executionScope) { this.executionScope = executionScope; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
