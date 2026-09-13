package com.b4rrhh.payroll_engine.metamodel.domain.model;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Construye metamodelos para los tests sin pasar por la base.
 *
 * <p>Las alimentaciones que se le pasan se dan por vigentes: el filtro por fecha lo hace la
 * carga, no el metamodelo, así que un test que quiera probar la vigencia debe probar la
 * consulta de carga, no esto.
 */
public final class RuleSystemMetamodelFixtures {

    private RuleSystemMetamodelFixtures() {
    }

    public static Builder metamodel(String ruleSystemCode, LocalDate referenceDate) {
        return new Builder(ruleSystemCode, referenceDate);
    }

    public static final class Builder {

        private final String ruleSystemCode;
        private final LocalDate referenceDate;
        private final List<PayrollConcept> concepts = new ArrayList<>();
        private final List<PayrollConceptOperand> operands = new ArrayList<>();
        private final List<PayrollConceptFeedRelation> feeds = new ArrayList<>();
        private final List<ConceptAssignment> assignments = new ArrayList<>();

        private Builder(String ruleSystemCode, LocalDate referenceDate) {
            this.ruleSystemCode = ruleSystemCode;
            this.referenceDate = referenceDate;
        }

        public Builder withConcepts(PayrollConcept... values) {
            concepts.addAll(Arrays.asList(values));
            return this;
        }

        public Builder withConcepts(List<PayrollConcept> values) {
            concepts.addAll(values);
            return this;
        }

        public Builder withOperands(PayrollConceptOperand... values) {
            operands.addAll(Arrays.asList(values));
            return this;
        }

        public Builder withOperands(List<PayrollConceptOperand> values) {
            operands.addAll(values);
            return this;
        }

        public Builder withFeeds(PayrollConceptFeedRelation... values) {
            feeds.addAll(Arrays.asList(values));
            return this;
        }

        public Builder withFeeds(List<PayrollConceptFeedRelation> values) {
            feeds.addAll(values);
            return this;
        }

        public Builder withAssignments(ConceptAssignment... values) {
            assignments.addAll(Arrays.asList(values));
            return this;
        }

        public Builder withAssignments(List<ConceptAssignment> values) {
            assignments.addAll(values);
            return this;
        }

        public RuleSystemMetamodel build() {
            return new RuleSystemMetamodel(
                    ruleSystemCode, referenceDate, concepts, operands, feeds, assignments);
        }
    }
}
