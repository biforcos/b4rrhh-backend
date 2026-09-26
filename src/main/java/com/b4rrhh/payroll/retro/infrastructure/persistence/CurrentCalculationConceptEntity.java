package com.b4rrhh.payroll.retro.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Una linea del calculo vigente ({@code payroll.current_calculation_concept}, V160). */
@Entity
@Table(name = "current_calculation_concept", schema = "payroll")
public class CurrentCalculationConceptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "current_calculation_id", nullable = false)
    private CurrentCalculationEntity currentCalculation;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "concept_code", nullable = false, length = 30)
    private String conceptCode;

    @Column(name = "concept_mnemonic", length = 50)
    private String conceptMnemonic;

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

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "payslip_section_code", length = 30)
    private String payslipSectionCode;

    @Column(name = "payslip_subsection_code", length = 30)
    private String payslipSubsectionCode;

    public Long getId() { return id; }

    public CurrentCalculationEntity getCurrentCalculation() { return currentCalculation; }
    public void setCurrentCalculation(CurrentCalculationEntity v) { this.currentCalculation = v; }

    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer v) { this.lineNumber = v; }

    public String getConceptCode() { return conceptCode; }
    public void setConceptCode(String v) { this.conceptCode = v; }

    public String getConceptMnemonic() { return conceptMnemonic; }
    public void setConceptMnemonic(String v) { this.conceptMnemonic = v; }

    public String getConceptLabel() { return conceptLabel; }
    public void setConceptLabel(String v) { this.conceptLabel = v; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal v) { this.rate = v; }

    public String getConceptNatureCode() { return conceptNatureCode; }
    public void setConceptNatureCode(String v) { this.conceptNatureCode = v; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer v) { this.displayOrder = v; }

    public String getPayslipSectionCode() { return payslipSectionCode; }
    public void setPayslipSectionCode(String v) { this.payslipSectionCode = v; }

    public String getPayslipSubsectionCode() { return payslipSubsectionCode; }
    public void setPayslipSubsectionCode(String v) { this.payslipSubsectionCode = v; }
}
