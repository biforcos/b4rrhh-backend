package com.b4rrhh.payroll.retro.infrastructure.persistence;

import com.b4rrhh.payroll.retro.domain.model.RetroMarkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** La marca de retroactividad en la base ({@code payroll.retro_mark}, V159). */
@Entity
@Table(name = "retro_mark", schema = "payroll")
public class RetroMarkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_system_code", nullable = false, length = 10)
    private String ruleSystemCode;

    @Column(name = "employee_type_code", nullable = false, length = 30)
    private String employeeTypeCode;

    @Column(name = "employee_number", nullable = false, length = 20)
    private String employeeNumber;

    @Column(name = "presence_number", nullable = false)
    private Integer presenceNumber;

    @Column(name = "from_period_code", nullable = false, length = 30)
    private String fromPeriodCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RetroMarkStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "source_vertical_code", nullable = false, length = 40)
    private String sourceVerticalCode;

    @Column(name = "source_table", nullable = false, length = 80)
    private String sourceTable;

    @Column(name = "source_row_id")
    private Long sourceRowId;

    @Column(name = "source_row_key", length = 200)
    private String sourceRowKey;

    @Column(name = "discarded_at")
    private Instant discardedAt;

    @Column(name = "discarded_by", length = 120)
    private String discardedBy;

    @Column(name = "discard_reason", length = 500)
    private String discardReason;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "consumed_period_code", length = 30)
    private String consumedPeriodCode;

    @Column(name = "consumed_run_id")
    private Long consumedRunId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String v) { this.ruleSystemCode = v; }

    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public void setEmployeeTypeCode(String v) { this.employeeTypeCode = v; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String v) { this.employeeNumber = v; }

    public Integer getPresenceNumber() { return presenceNumber; }
    public void setPresenceNumber(Integer v) { this.presenceNumber = v; }

    public String getFromPeriodCode() { return fromPeriodCode; }
    public void setFromPeriodCode(String v) { this.fromPeriodCode = v; }

    public RetroMarkStatus getStatus() { return status; }
    public void setStatus(RetroMarkStatus v) { this.status = v; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }

    public String getSourceVerticalCode() { return sourceVerticalCode; }
    public void setSourceVerticalCode(String v) { this.sourceVerticalCode = v; }

    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String v) { this.sourceTable = v; }

    public Long getSourceRowId() { return sourceRowId; }
    public void setSourceRowId(Long v) { this.sourceRowId = v; }

    public String getSourceRowKey() { return sourceRowKey; }
    public void setSourceRowKey(String v) { this.sourceRowKey = v; }

    public Instant getDiscardedAt() { return discardedAt; }
    public void setDiscardedAt(Instant v) { this.discardedAt = v; }

    public String getDiscardedBy() { return discardedBy; }
    public void setDiscardedBy(String v) { this.discardedBy = v; }

    public String getDiscardReason() { return discardReason; }
    public void setDiscardReason(String v) { this.discardReason = v; }

    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant v) { this.consumedAt = v; }

    public String getConsumedPeriodCode() { return consumedPeriodCode; }
    public void setConsumedPeriodCode(String v) { this.consumedPeriodCode = v; }

    public Long getConsumedRunId() { return consumedRunId; }
    public void setConsumedRunId(Long v) { this.consumedRunId = v; }
}
