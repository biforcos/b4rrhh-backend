package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.support.TestPostgresInitializer;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
// Postgres de verdad en vez del H2 que Spring pone por defecto. Este test
// aplica un subconjunto de las migraciones reales sobre una base virgen que le
// prepara el initializer, asi que hasta ahora estaba ejecutando SQL de
// produccion contra un motor que no es el de produccion.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = TestPostgresInitializer.class)
class PayrollConceptOperandPersistenceTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private SpringDataPayrollObjectRepository objectRepository;

    @Autowired
    private SpringDataPayrollConceptOperandRepository operandRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path dir = Files.createDirectories(tempDir.resolve("flyway-operand"));
        copyMigration(dir, "V56__create_payroll_engine_schema.sql");
        copyMigration(dir, "V57__add_payroll_concept_operand.sql");
        copyMigration(dir, "V80__add_persist_to_concepts_to_payroll_concept.sql");
        copyMigration(dir, "V81__add_summary_to_payroll_concept.sql");
        registry.add("spring.flyway.locations", () -> "filesystem:" + dir.toAbsolutePath());
    }

    private static void copyMigration(Path directory, String filename) throws IOException {
        try (InputStream is = PayrollConceptOperandPersistenceTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + filename)) {
            if (is == null) {
                throw new IllegalStateException("Migration not found: " + filename);
            }
            Files.copy(is, directory.resolve(filename));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PayrollObjectEntity saveObject(String ruleSystem, String typeCode, String objectCode) {
        PayrollObjectEntity e = new PayrollObjectEntity();
        e.setRuleSystemCode(ruleSystem);
        e.setObjectTypeCode(typeCode);
        e.setObjectCode(objectCode);
        return objectRepository.saveAndFlush(e);
    }

    private PayrollConceptOperandEntity buildOperand(
            PayrollObjectEntity target,
            String role,
            PayrollObjectEntity source
    ) {
        PayrollConceptOperandEntity e = new PayrollConceptOperandEntity();
        e.setTargetObject(target);
        e.setOperandRole(role);
        e.setSourceObject(source);
        return e;
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void persistsAndRetrievesOperand() {
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "SALARIO_BASE");
        PayrollObjectEntity source = saveObject("ESP", "CONCEPT", "T_DIAS_PRESENCIA_SEGMENTO");

        operandRepository.saveAndFlush(buildOperand(target, "QUANTITY", source));

        List<PayrollConceptOperandEntity> results =
                operandRepository.findByTargetObject_RuleSystemCodeAndTargetObject_ObjectCode(
                        "ESP", "SALARIO_BASE");

        assertEquals(1, results.size());
        assertEquals("QUANTITY", results.get(0).getOperandRole());
        assertEquals("T_DIAS_PRESENCIA_SEGMENTO", results.get(0).getSourceObject().getObjectCode());
    }

    @Test
    void persistsTwoRolesForSameTarget() {
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "SAL_BASE_2");
        PayrollObjectEntity sourceQ = saveObject("ESP", "CONCEPT", "T_DIAS_SEG_2");
        PayrollObjectEntity sourceR = saveObject("ESP", "CONCEPT", "T_PRECIO_DIA_2");

        operandRepository.saveAndFlush(buildOperand(target, "QUANTITY", sourceQ));
        operandRepository.saveAndFlush(buildOperand(target, "RATE", sourceR));

        List<PayrollConceptOperandEntity> results =
                operandRepository.findByTargetObject_RuleSystemCodeAndTargetObject_ObjectCode(
                        "ESP", "SAL_BASE_2");

        assertEquals(2, results.size());
    }

    @Test
    void rejectsNullOperandRole() {
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "SAL_NULL_ROLE");
        PayrollObjectEntity source = saveObject("ESP", "CONCEPT", "T_NULL_SRC");

        PayrollConceptOperandEntity bad = buildOperand(target, null, source);
        assertThrows(Exception.class, () -> operandRepository.saveAndFlush(bad));
    }

    @Test
    void enforcesUniqueConstraintOnTargetAndRole() {
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "SAL_DUP");
        PayrollObjectEntity source1 = saveObject("ESP", "CONCEPT", "T_SRC_DUP1");
        PayrollObjectEntity source2 = saveObject("ESP", "CONCEPT", "T_SRC_DUP2");

        operandRepository.saveAndFlush(buildOperand(target, "QUANTITY", source1));

        assertThrows(DataIntegrityViolationException.class,
                () -> operandRepository.saveAndFlush(buildOperand(target, "QUANTITY", source2)));
    }
}
