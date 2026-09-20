package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * En que seccion va una naturaleza.
 *
 * <p>La clave es (sistema de reglas, naturaleza) y no lleva la seccion dentro: una naturaleza
 * esta en UNA seccion, o la pregunta «donde va esta linea» tendria dos respuestas.
 */
@Entity(name = "PayrollEnginePayslipSectionNatureEntity")
@Table(name = "payslip_section_nature", schema = "payroll_engine")
@IdClass(PayslipSectionNatureEntity.Key.class)
public class PayslipSectionNatureEntity {

    @Id
    @Column(name = "rule_system_code", nullable = false, length = 10)
    private String ruleSystemCode;

    @Id
    @Column(name = "functional_nature", nullable = false, length = 30)
    private String functionalNature;

    @Column(name = "section_code", nullable = false, length = 30)
    private String sectionCode;

    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String ruleSystemCode) { this.ruleSystemCode = ruleSystemCode; }

    public String getFunctionalNature() { return functionalNature; }
    public void setFunctionalNature(String functionalNature) { this.functionalNature = functionalNature; }

    public String getSectionCode() { return sectionCode; }
    public void setSectionCode(String sectionCode) { this.sectionCode = sectionCode; }

    public static class Key implements Serializable {
        private String ruleSystemCode;
        private String functionalNature;

        public Key() {
        }

        public Key(String ruleSystemCode, String functionalNature) {
            this.ruleSystemCode = ruleSystemCode;
            this.functionalNature = functionalNature;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(ruleSystemCode, key.ruleSystemCode)
                    && Objects.equals(functionalNature, key.functionalNature);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleSystemCode, functionalNature);
        }
    }
}
