package com.b4rrhh.payroll.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "payroll_concept", schema = "payroll")
public class PayrollConceptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_id", nullable = false)
    private PayrollEntity payroll;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "concept_code", nullable = false, length = 30)
    private String conceptCode;

    /** Lo que las reglas y el grafo usan. Va junto al literal, no en su lugar (backend#109). */
    @Column(name = "concept_mnemonic", nullable = false, length = 50)
    private String conceptMnemonic;

    /** Como se llamaba el concepto al calcular esta linea. Congelado, no resuelto al leer. */
    @Column(name = "concept_label", nullable = false, length = 200)
    private String conceptLabel;

    @Column(name = "amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(name = "quantity", precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "rate", precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(name = "concept_nature_code", nullable = false, length = 30)
    private String conceptNatureCode;

    @Column(name = "origin_period_code", length = 30)
    private String originPeriodCode;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /** El bloque del modelo oficial en el que salio esta linea. Nulo si la naturaleza no tenia. */
    @Column(name = "payslip_section_code", length = 30)
    private String payslipSectionCode;

    /** El apartado del bloque, cuando el bloque tiene apartados (backend#121). */
    @Column(name = "payslip_subsection_code", length = 30)
    private String payslipSubsectionCode;

    /** De cuantos pasos del motor viene esta linea. Uno salvo que el folio haya fundido. */
    @Column(name = "merged_step_count", nullable = false)
    private Integer mergedStepCount = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PayrollEntity getPayroll() { return payroll; }
    public void setPayroll(PayrollEntity payroll) { this.payroll = payroll; }
    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
    public String getConceptCode() { return conceptCode; }
    public void setConceptCode(String conceptCode) { this.conceptCode = conceptCode; }
    public String getConceptMnemonic() { return conceptMnemonic; }
    public void setConceptMnemonic(String conceptMnemonic) { this.conceptMnemonic = conceptMnemonic; }
    public String getConceptLabel() { return conceptLabel; }
    public void setConceptLabel(String conceptLabel) { this.conceptLabel = conceptLabel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public String getConceptNatureCode() { return conceptNatureCode; }
    public void setConceptNatureCode(String conceptNatureCode) { this.conceptNatureCode = conceptNatureCode; }
    public String getOriginPeriodCode() { return originPeriodCode; }
    public void setOriginPeriodCode(String originPeriodCode) { this.originPeriodCode = originPeriodCode; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public String getPayslipSectionCode() { return payslipSectionCode; }
    public void setPayslipSectionCode(String payslipSectionCode) { this.payslipSectionCode = payslipSectionCode; }

    public String getPayslipSubsectionCode() { return payslipSubsectionCode; }
    public void setPayslipSubsectionCode(String payslipSubsectionCode) { this.payslipSubsectionCode = payslipSubsectionCode; }
    public Integer getMergedStepCount() { return mergedStepCount; }
    public void setMergedStepCount(Integer mergedStepCount) { this.mergedStepCount = mergedStepCount; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PayrollConceptEntity that)) {
            return false;
        }
        return Objects.equals(payroll, that.payroll)
                && Objects.equals(lineNumber, that.lineNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payroll, lineNumber);
    }
}