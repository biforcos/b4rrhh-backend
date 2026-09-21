package com.b4rrhh.payroll_engine.eligibility.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity(name = "PayrollEngineConceptAssignmentEntity")
@Table(name = "concept_assignment", schema = "payroll_engine")
public class ConceptAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_system_code", nullable = false, length = 30)
    private String ruleSystemCode;

    @Column(name = "concept_code", nullable = false, length = 100)
    private String conceptCode;

    @Column(name = "company_code", length = 30)
    private String companyCode;

    @Column(name = "agreement_code", length = 30)
    private String agreementCode;

    @Column(name = "employee_type_code", length = 30)
    private String employeeTypeCode;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Instant y no LocalDateTime, y su hermana createdAt sigue siendo LocalDateTime: esta
    // columna entra en la comparacion del RuleSystemLastChangeLookupAdapter y la otra no.
    // La asimetria es el alcance del backend#116, no un descuido.
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        Instant ahora = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = ahora;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String ruleSystemCode) { this.ruleSystemCode = ruleSystemCode; }

    public String getConceptCode() { return conceptCode; }
    public void setConceptCode(String conceptCode) { this.conceptCode = conceptCode; }

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }

    public String getAgreementCode() { return agreementCode; }
    public void setAgreementCode(String agreementCode) { this.agreementCode = agreementCode; }

    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public void setEmployeeTypeCode(String employeeTypeCode) { this.employeeTypeCode = employeeTypeCode; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
