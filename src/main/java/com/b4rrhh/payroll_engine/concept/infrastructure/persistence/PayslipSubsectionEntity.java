package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * Una parte de un bloque del recibo ({@code backend#121}).
 *
 * <p>La clave es (sistema de reglas, apartado) y no incluye el bloque: un apartado esta en UN
 * bloque, igual que una naturaleza esta en UNA seccion.
 */
@Entity(name = "PayrollEnginePayslipSubsectionEntity")
@Table(name = "payslip_subsection", schema = "payroll_engine")
@IdClass(PayslipSubsectionEntity.Key.class)
public class PayslipSubsectionEntity {

    @Id
    @Column(name = "rule_system_code", nullable = false, length = 10)
    private String ruleSystemCode;

    @Id
    @Column(name = "subsection_code", nullable = false, length = 30)
    private String subsectionCode;

    @Column(name = "section_code", nullable = false, length = 30)
    private String sectionCode;

    @Column(name = "subsection_label", nullable = false, length = 200)
    private String subsectionLabel;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String ruleSystemCode) { this.ruleSystemCode = ruleSystemCode; }

    public String getSubsectionCode() { return subsectionCode; }
    public void setSubsectionCode(String subsectionCode) { this.subsectionCode = subsectionCode; }

    public String getSectionCode() { return sectionCode; }
    public void setSectionCode(String sectionCode) { this.sectionCode = sectionCode; }

    public String getSubsectionLabel() { return subsectionLabel; }
    public void setSubsectionLabel(String subsectionLabel) { this.subsectionLabel = subsectionLabel; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public static class Key implements Serializable {
        private String ruleSystemCode;
        private String subsectionCode;

        public Key() {
        }

        public Key(String ruleSystemCode, String subsectionCode) {
            this.ruleSystemCode = ruleSystemCode;
            this.subsectionCode = subsectionCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(ruleSystemCode, key.ruleSystemCode)
                    && Objects.equals(subsectionCode, key.subsectionCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleSystemCode, subsectionCode);
        }
    }
}
