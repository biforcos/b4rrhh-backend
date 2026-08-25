package com.b4rrhh.payroll_engine.concept.domain.port;

import com.b4rrhh.support.EsquemaRealInitializer;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.ResultCompositionMode;
import com.b4rrhh.payroll_engine.concept.infrastructure.persistence.PayrollConceptPersistenceAdapter;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link PayrollConceptRepository} covering the
 * {@code findAllByRuleSystemCode} and {@code deleteByBusinessKey} operations.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). El subconjunto de
// migraciones que se copiaba aqui estaba congelado: una migracion futura que
// tocara estas tablas no se veia.
// El sistema de reglas no puede ser ESP: las migraciones siembran los
// conceptos 101 y D01 para ESP, que son justo los que crea este test, y
// findAllByRuleSystemCode devolveria tambien las semillas. TST no lo
// siembra nadie.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
@Import(PayrollConceptPersistenceAdapter.class)
class PayrollConceptRepositoryFindAllTest {

    @Autowired
    private PayrollConceptRepository repository;

    @Autowired
    private SpringDataPayrollObjectRepository objectRepository;

    @Test
    void findAllByRuleSystemCode_returnsAllConceptsForRuleSystem() {
        seedConcept("TST", "101", "SALARIO_BASE");
        seedConcept("TST", "D01", "DIAS_MES");
        seedConcept("OTHER", "X99", "OTHER_CONCEPT");

        List<PayrollConcept> concepts = repository.findAllByRuleSystemCode("TST");

        assertThat(concepts).isNotEmpty();
        assertThat(concepts).allMatch(c -> c.getRuleSystemCode().equals("TST"));
        assertThat(concepts).extracting(PayrollConcept::getConceptCode)
                .containsExactly("101", "D01");
    }

    @Test
    void findAllByRuleSystemCode_returnsEmptyForUnknownSystem() {
        seedConcept("TST", "101", "SALARIO_BASE");

        List<PayrollConcept> concepts = repository.findAllByRuleSystemCode("UNKNOWN");

        assertThat(concepts).isEmpty();
    }

    @Test
    void deleteByBusinessKey_removesConceptFromRepository() {
        seedConcept("TST", "101", "SALARIO_BASE");
        seedConcept("TST", "D01", "DIAS_MES");

        List<PayrollConcept> before = repository.findAllByRuleSystemCode("TST");
        assertThat(before).extracting(PayrollConcept::getConceptCode)
                .contains("101");

        repository.deleteByBusinessKey("TST", "101");

        assertThat(repository.findAllByRuleSystemCode("TST"))
                .noneMatch(c -> c.getConceptCode().equals("101"));
    }

    @Test
    void deleteByBusinessKey_noOpForNonExistentConcept() {
        seedConcept("TST", "101", "SALARIO_BASE");

        assertThatNoException().isThrownBy(
                () -> repository.deleteByBusinessKey("TST", "NONEXISTENT")
        );

        // Existing concept must still be present
        assertThat(repository.findAllByRuleSystemCode("TST"))
                .extracting(PayrollConcept::getConceptCode)
                .contains("101");
    }

    private void seedConcept(String ruleSystemCode, String conceptCode, String mnemonic) {
        PayrollObjectEntity objectEntity = new PayrollObjectEntity();
        objectEntity.setRuleSystemCode(ruleSystemCode);
        objectEntity.setObjectTypeCode(PayrollObjectTypeCode.CONCEPT.name());
        objectEntity.setObjectCode(conceptCode);
        PayrollObjectEntity saved = objectRepository.saveAndFlush(objectEntity);

        PayrollObject domainObject = new PayrollObject(
                saved.getId(),
                saved.getRuleSystemCode(),
                PayrollObjectTypeCode.valueOf(saved.getObjectTypeCode()),
                saved.getObjectCode(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
        PayrollConcept concept = new PayrollConcept(
                domainObject,
                mnemonic,
                CalculationType.DIRECT_AMOUNT,
                FunctionalNature.EARNING,
                ResultCompositionMode.REPLACE,
                null,
                ExecutionScope.PERIOD,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        repository.save(concept);
    }
}
