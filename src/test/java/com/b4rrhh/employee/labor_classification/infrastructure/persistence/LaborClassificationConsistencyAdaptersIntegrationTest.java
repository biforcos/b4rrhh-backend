package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// El esquema es el real, con sus semillas: los codigos de convenio y categoria
// de estos tests son propios (ZAG1/ZCA1/ZCA2) porque AGR_OFFICE o CAT_ADMIN ya
// vienen sembrados con sus relaciones (V25/V52) y contra esos la asercion
// negativa no demostraria nada.
class LaborClassificationConsistencyAdaptersIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LaborClassificationPresenceConsistencyAdapter presenceConsistencyAdapter;
    private AgreementCategoryRelationLookupAdapter relationLookupAdapter;

    @BeforeEach
    void setUp() {
        presenceConsistencyAdapter = new LaborClassificationPresenceConsistencyAdapter(entityManager);
        relationLookupAdapter = new AgreementCategoryRelationLookupAdapter(entityManager);
    }

    @Test
    void presenceContainmentQueryIsCorrect() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        DatosDePrueba.presencia(jdbcTemplate, empleado, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        DatosDePrueba.presencia(jdbcTemplate, empleado, 2, LocalDate.of(2026, 2, 10), null);

        boolean inside = presenceConsistencyAdapter.existsPresenceContainingPeriod(
                empleado,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 20)
        );
        boolean outside = presenceConsistencyAdapter.existsPresenceContainingPeriod(
                empleado,
                LocalDate.of(2026, 1, 25),
                LocalDate.of(2026, 2, 12)
        );

        assertTrue(inside);
        assertFalse(outside);
        assertEquals(2, presenceConsistencyAdapter.findPresencePeriodsByEmployeeIdOrderByStartDate(empleado).size());
    }

    @Test
    void agreementCategoryRelationLookupIsCorrect() {
        seedAgreementCategoryRelationData();

        boolean validRelation = relationLookupAdapter.existsActiveRelation(
                "ESP",
                "ZAG1",
                "ZCA1",
                LocalDate.of(2026, 1, 1)
        );
        boolean invalidRelation = relationLookupAdapter.existsActiveRelation(
                "ESP",
                "ZAG1",
                "ZCA2",
                LocalDate.of(2026, 1, 1)
        );

        assertTrue(validRelation);
        assertFalse(invalidRelation);
    }

    @Test
    void agreementCategoryRelationLookupHandlesNullReferenceDateWithoutSqlGrammarError() {
        seedAgreementCategoryRelationData();

        Boolean validRelation = assertDoesNotThrow(() -> relationLookupAdapter.existsActiveRelation(
                "ESP",
                "ZAG1",
                "ZCA1",
                null
        ));
        Boolean invalidRelation = assertDoesNotThrow(() -> relationLookupAdapter.existsActiveRelation(
                "ESP",
                "ZAG1",
                "ZCA2",
                null
        ));

        assertTrue(validRelation);
        assertFalse(invalidRelation);
    }

    private void seedAgreementCategoryRelationData() {
        Long ruleSystemId = jdbcTemplate.queryForObject(
                "select id from rulesystem.rule_system where code = 'ESP'",
                Long.class
        );
        Long convenio = DatosDePrueba.ruleEntity(
                jdbcTemplate, "AGREEMENT", "ZAG1", "Test Agreement",
                LocalDate.of(1900, 1, 1), null
        );
        Long categoriaRelacionada = DatosDePrueba.ruleEntity(
                jdbcTemplate, "AGREEMENT_CATEGORY", "ZCA1", "Related Category",
                LocalDate.of(1900, 1, 1), null
        );
        DatosDePrueba.ruleEntity(
                jdbcTemplate, "AGREEMENT_CATEGORY", "ZCA2", "Unrelated Category",
                LocalDate.of(1900, 1, 1), null
        );

        jdbcTemplate.update(
                """
                insert into rulesystem.agreement_category_relation (
                    rule_system_id, agreement_rule_entity_id, category_rule_entity_id, start_date, end_date, is_active
                ) values (?,?,?,?,?,?)
                """,
                ruleSystemId,
                convenio,
                categoriaRelacionada,
                LocalDate.of(1900, 1, 1),
                null,
                true
        );
    }
}
