package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.support.EsquemaRealInitializer;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). El subconjunto de
// migraciones que se copiaba aqui estaba congelado: una migracion futura que
// tocara estas tablas no se veia.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
class SpringDataPayrollRepositoryFlywayIntegrationTest {

    @Autowired
    private SpringDataPayrollRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesFlywayMigrationAndLoadsAggregateByBusinessKey() {
        repository.saveAndFlush(payrollEntity());

        Optional<PayrollEntity> result = repository
                .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumberAndPayrollPeriodCodeAndPayrollTypeCodeAndPresenceNumber(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        "202501",
                        "NORMAL",
                        1
                );

        assertTrue(result.isPresent());
        assertEquals(PayrollStatus.CALCULATED, result.get().getStatus());
        assertEquals(1, result.get().getConcepts().size());
        assertEquals(1, result.get().getContextSnapshots().size());
    }

    @Test
    void enforcesUniqueBusinessKeyConstraint() {
        repository.saveAndFlush(payrollEntity());

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(payrollEntity()));
    }

        @Test
        void enforcesUniqueConceptLineNumberPerPayroll() {
        PayrollEntity saved = repository.saveAndFlush(payrollEntity());

        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "insert into payroll.payroll_concept (payroll_id, line_number, concept_code, concept_label, amount, concept_nature_code, display_order) values (?, ?, ?, ?, ?, ?, ?)",
                saved.getId(),
                1,
                "BONUS",
                "Duplicate line",
                new BigDecimal("10.00"),
                "EARNING",
                2
            )
        );
        }

        @Test
        void enforcesUniqueSnapshotTypePerPayroll() {
        PayrollEntity saved = repository.saveAndFlush(payrollEntity());

        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "insert into payroll.payroll_context_snapshot (payroll_id, snapshot_type_code, source_vertical_code, source_business_key_json, snapshot_payload_json) values (?, ?, ?, cast(? as json), cast(? as json))",
                saved.getId(),
                "PRESENCE",
                "EMPLOYEE",
                "{\"presenceNumber\":1}",
                "{\"companyCode\":\"ES01\"}"
            )
        );
        }

    private PayrollEntity payrollEntity() {
        PayrollEntity payroll = new PayrollEntity();
        payroll.setRuleSystemCode("ESP");
        payroll.setEmployeeTypeCode("INTERNAL");
        payroll.setEmployeeNumber("EMP001");
        payroll.setPayrollPeriodCode("202501");
        payroll.setPayrollTypeCode("NORMAL");
        payroll.setPresenceNumber(1);
        payroll.setStatus(PayrollStatus.CALCULATED);
        payroll.setCalculatedAt(LocalDateTime.of(2026, 1, 31, 10, 15));
        payroll.setCalculationEngineCode("ENGINE");
        payroll.setCalculationEngineVersion("1.0");

        PayrollConceptEntity concept = new PayrollConceptEntity();
        concept.setLineNumber(1);
        concept.setConceptCode("BASE");
        concept.setConceptLabel("Base salary");
        concept.setAmount(new BigDecimal("1000.00"));
        concept.setConceptNatureCode("EARNING");
        concept.setOriginPeriodCode("202501");
        concept.setDisplayOrder(1);

        PayrollContextSnapshotEntity snapshot = new PayrollContextSnapshotEntity();
        snapshot.setSnapshotTypeCode("PRESENCE");
        snapshot.setSourceVerticalCode("EMPLOYEE");
        snapshot.setSourceBusinessKeyJson("{\"presenceNumber\":1}");
        snapshot.setSnapshotPayloadJson("{\"companyCode\":\"ES01\"}");

        payroll.replaceConcepts(List.of(concept));
        payroll.replaceContextSnapshots(List.of(snapshot));
        return payroll;
    }
}