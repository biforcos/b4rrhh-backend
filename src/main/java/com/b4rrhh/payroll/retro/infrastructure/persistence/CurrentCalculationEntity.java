package com.b4rrhh.payroll.retro.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** El calculo vigente en la base ({@code payroll.current_calculation}, V160). */
@Entity
@Table(name = "current_calculation", schema = "payroll")
public class CurrentCalculationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_system_code", nullable = false, length = 10)
    private String ruleSystemCode;

    @Column(name = "employee_type_code", nullable = false, length = 30)
    private String employeeTypeCode;

    @Column(name = "employee_number", nullable = false, length = 20)
    private String employeeNumber;

    @Column(name = "payroll_period_code", nullable = false, length = 30)
    private String payrollPeriodCode;

    @Column(name = "payroll_type_code", nullable = false, length = 30)
    private String payrollTypeCode;

    @Column(name = "presence_number", nullable = false)
    private Integer presenceNumber;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "currentCalculation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber asc")
    private List<CurrentCalculationConceptEntity> concepts = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String v) { this.ruleSystemCode = v; }

    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public void setEmployeeTypeCode(String v) { this.employeeTypeCode = v; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String v) { this.employeeNumber = v; }

    public String getPayrollPeriodCode() { return payrollPeriodCode; }
    public void setPayrollPeriodCode(String v) { this.payrollPeriodCode = v; }

    public String getPayrollTypeCode() { return payrollTypeCode; }
    public void setPayrollTypeCode(String v) { this.payrollTypeCode = v; }

    public Integer getPresenceNumber() { return presenceNumber; }
    public void setPresenceNumber(Integer v) { this.presenceNumber = v; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant v) { this.calculatedAt = v; }

    public Long getRunId() { return runId; }
    public void setRunId(Long v) { this.runId = v; }

    public List<CurrentCalculationConceptEntity> getConcepts() { return concepts; }

    /** Pisa las lineas: el vigente no acumula, se sustituye entero. */
    public void replaceConcepts(List<CurrentCalculationConceptEntity> nuevas) {
        this.concepts.clear();
        for (CurrentCalculationConceptEntity c : nuevas) {
            c.setCurrentCalculation(this);
            this.concepts.add(c);
        }
    }
}
