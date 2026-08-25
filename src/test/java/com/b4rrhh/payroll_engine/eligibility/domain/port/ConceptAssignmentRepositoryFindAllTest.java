package com.b4rrhh.payroll_engine.eligibility.domain.port;

import com.b4rrhh.support.TestSobreEsquemaReal;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link ConceptAssignmentRepository} covering the
 * {@code findAllByRuleSystemCode}, {@code findAllByRuleSystemCodeAndConceptCode}
 * and {@code deleteById} operations.
 */
@TestSobreEsquemaReal
// El sistema de reglas no puede ser ESP: las migraciones siembran
// concept_assignment para ESP y findAllByRuleSystemCode devolveria tambien las
// semillas. TST no lo siembra nadie.
class ConceptAssignmentRepositoryFindAllTest {

    @Autowired
    private ConceptAssignmentRepository repository;

    private static final LocalDate JAN_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DEC_31 = LocalDate.of(2025, 12, 31);

    @Test
    void findAllByRuleSystemCode_returnsAllAssignmentsForRuleSystem() {
        repository.save(assignment("TST", "101", null, "AGR1", null, 10));
        repository.save(assignment("TST", "970", null, "AGR1", null, 20));
        repository.save(assignment("FRA", "X99", null, null, null, 30));

        List<ConceptAssignment> result = repository.findAllByRuleSystemCode("TST");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getRuleSystemCode().equals("TST"));
        assertThat(result).extracting(ConceptAssignment::getConceptCode)
                .containsExactlyInAnyOrder("101", "970");
    }

    @Test
    void findAllByRuleSystemCode_returnsEmptyForUnknownSystem() {
        repository.save(assignment("TST", "101", null, "AGR1", null, 10));

        List<ConceptAssignment> result = repository.findAllByRuleSystemCode("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByRuleSystemCodeAndConceptCode_returnsOnlyMatchingConcept() {
        repository.save(assignment("TST", "101", null, "AGR1", null, 10));
        repository.save(assignment("TST", "101", "EMP1", "AGR1", null, 50));
        repository.save(assignment("TST", "970", null, "AGR1", null, 20));

        List<ConceptAssignment> result = repository.findAllByRuleSystemCodeAndConceptCode("TST", "101");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getConceptCode().equals("101"));
        // ordered by priority DESC (50 first, then 10)
        assertThat(result.get(0).getPriority()).isEqualTo(50);
        assertThat(result.get(1).getPriority()).isEqualTo(10);
    }

    @Test
    void findAllByRuleSystemCodeAndConceptCode_returnsEmptyWhenConceptNotFound() {
        repository.save(assignment("TST", "101", null, "AGR1", null, 10));

        List<ConceptAssignment> result =
                repository.findAllByRuleSystemCodeAndConceptCode("TST", "NONEXISTENT");

        assertThat(result).isEmpty();
    }

    @Test
    void deleteById_removesAssignmentFromRepository() {
        ConceptAssignment saved = repository.save(assignment("TST", "101", null, "AGR1", null, 10));
        ConceptAssignment kept = repository.save(assignment("TST", "970", null, "AGR1", null, 20));

        assertThat(saved.getId()).isNotNull();

        repository.deleteById(saved.getId());

        List<ConceptAssignment> remaining = repository.findAllByRuleSystemCode("TST");
        assertThat(remaining).extracting(ConceptAssignment::getId)
                .containsExactly(kept.getId());
    }

    @Test
    void deleteById_isNoOpForNonExistentId() {
        repository.save(assignment("TST", "101", null, "AGR1", null, 10));

        assertThatNoException().isThrownBy(() -> repository.deleteById(999_999L));
        assertThatNoException().isThrownBy(() -> repository.deleteById(null));

        assertThat(repository.findAllByRuleSystemCode("TST")).hasSize(1);
    }

    private ConceptAssignment assignment(
            String ruleSystemCode, String conceptCode,
            String companyCode, String agreementCode, String employeeTypeCode,
            int priority
    ) {
        return new ConceptAssignment(
                null, ruleSystemCode, conceptCode,
                companyCode, agreementCode, employeeTypeCode,
                JAN_1, DEC_31, priority,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
