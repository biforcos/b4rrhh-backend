package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/** Un bloque del recibo. La clave es (sistema de reglas, codigo): no hay subrogada. */
@Entity(name = "PayrollEnginePayslipSectionEntity")
@Table(name = "payslip_section", schema = "payroll_engine")
@IdClass(PayslipSectionEntity.Key.class)
public class PayslipSectionEntity {

    @Id
    @Column(name = "rule_system_code", nullable = false, length = 10)
    private String ruleSystemCode;

    @Id
    @Column(name = "section_code", nullable = false, length = 30)
    private String sectionCode;

    @Column(name = "section_label", nullable = false, length = 200)
    private String sectionLabel;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String ruleSystemCode) { this.ruleSystemCode = ruleSystemCode; }

    public String getSectionCode() { return sectionCode; }
    public void setSectionCode(String sectionCode) { this.sectionCode = sectionCode; }

    public String getSectionLabel() { return sectionLabel; }
    public void setSectionLabel(String sectionLabel) { this.sectionLabel = sectionLabel; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public static class Key implements Serializable {
        private String ruleSystemCode;
        private String sectionCode;

        public Key() {
        }

        public Key(String ruleSystemCode, String sectionCode) {
            this.ruleSystemCode = ruleSystemCode;
            this.sectionCode = sectionCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(ruleSystemCode, key.ruleSystemCode)
                    && Objects.equals(sectionCode, key.sectionCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleSystemCode, sectionCode);
        }
    }
}
