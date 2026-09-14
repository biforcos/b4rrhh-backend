package com.b4rrhh.payroll.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un paso del cálculo de un recibo, en su propia tabla.
 *
 * <p>No cuelga del agregado {@code PayrollEntity} y no hay {@code @ManyToOne} hacia él: los pasos
 * no son líneas de recibo y no se cargan con el recibo. Lo que los ata es la clave ajena con
 * {@code on delete cascade}, que es lo que hace que un recálculo no pueda dejar un recibo nuevo con
 * pasos viejos ({@code backend#93}).
 */
@Entity
@IdClass(PayrollCalculationStepEntityId.class)
@Table(name = "payroll_calculation_step", schema = "payroll")
public class PayrollCalculationStepEntity {

    @Id
    @Column(name = "payroll_id", nullable = false)
    private Long payrollId;

    @Id
    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder;

    @Column(name = "concept_code", nullable = false, length = 30)
    private String conceptCode;

    @Column(name = "concept_mnemonic", nullable = false, length = 50)
    private String conceptMnemonic;

    @Column(name = "calculation_type", nullable = false, length = 30)
    private String calculationType;

    @Column(name = "functional_nature", nullable = false, length = 30)
    private String functionalNature;

    @Column(name = "execution_scope", nullable = false, length = 30)
    private String executionScope;

    @Column(name = "segment_start_date")
    private LocalDate segmentStartDate;

    @Column(name = "segment_end_date")
    private LocalDate segmentEndDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(name = "quantity", precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "rate", precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(name = "payslip_order_code", length = 30)
    private String payslipOrderCode;

    public Long getPayrollId() { return payrollId; }
    public void setPayrollId(Long payrollId) { this.payrollId = payrollId; }
    public Integer getExecutionOrder() { return executionOrder; }
    public void setExecutionOrder(Integer executionOrder) { this.executionOrder = executionOrder; }
    public String getConceptCode() { return conceptCode; }
    public void setConceptCode(String conceptCode) { this.conceptCode = conceptCode; }
    public String getConceptMnemonic() { return conceptMnemonic; }
    public void setConceptMnemonic(String conceptMnemonic) { this.conceptMnemonic = conceptMnemonic; }
    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }
    public String getFunctionalNature() { return functionalNature; }
    public void setFunctionalNature(String functionalNature) { this.functionalNature = functionalNature; }
    public String getExecutionScope() { return executionScope; }
    public void setExecutionScope(String executionScope) { this.executionScope = executionScope; }
    public LocalDate getSegmentStartDate() { return segmentStartDate; }
    public void setSegmentStartDate(LocalDate segmentStartDate) { this.segmentStartDate = segmentStartDate; }
    public LocalDate getSegmentEndDate() { return segmentEndDate; }
    public void setSegmentEndDate(LocalDate segmentEndDate) { this.segmentEndDate = segmentEndDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public String getPayslipOrderCode() { return payslipOrderCode; }
    public void setPayslipOrderCode(String payslipOrderCode) { this.payslipOrderCode = payslipOrderCode; }
}
