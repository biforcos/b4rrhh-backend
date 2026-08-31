package com.b4rrhh.employee.contract.infrastructure.persistence;

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
// El esquema es el real, con sus semillas: los codigos de contrato y subtipo
// de estos tests son propios (ZC1/ZS1/ZS2) porque IND, TMP, FT1 y PT1 ya
// vienen sembrados con sus relaciones (V29/V52) y contra esos la asercion
// negativa no demostraria nada.
class ContractConsistencyAdaptersIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ContractPresenceConsistencyAdapter presenceConsistencyAdapter;
    private ContractSubtypeRelationLookupAdapter relationLookupAdapter;

    @BeforeEach
    void setUp() {
        presenceConsistencyAdapter = new ContractPresenceConsistencyAdapter(entityManager);
        relationLookupAdapter = new ContractSubtypeRelationLookupAdapter(entityManager);
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
    void contractSubtypeRelationLookupIsCorrect() {
        seedContractSubtypeRelationData();

        boolean validRelation = relationLookupAdapter.existsActiveRelation(
                "ESP",
                "ZC1",
                "ZS1",
                LocalDate.of(2026, 1, 1)
        );
        boolean invalidRelation = relationLookupAdapter.existsActiveRelation(
                "ESP",
                "ZC1",
                "ZS2",
                LocalDate.of(2026, 1, 1)
        );

        assertTrue(validRelation);
        assertFalse(invalidRelation);
    }

    @Test
    void contractSubtypeRelationLookupHandlesNullReferenceDateWithoutSqlGrammarError() {
        seedContractSubtypeRelationData();

        Boolean validRelation = assertDoesNotThrow(() -> relationLookupAdapter.existsActiveRelation(
                "ESP",
                "ZC1",
                "ZS1",
                null
        ));
        Boolean invalidRelation = assertDoesNotThrow(() -> relationLookupAdapter.existsActiveRelation(
                "ESP",
                "ZC1",
                "ZS2",
                null
        ));

        assertTrue(validRelation);
        assertFalse(invalidRelation);
    }

    private void seedContractSubtypeRelationData() {
        Long ruleSystemId = jdbcTemplate.queryForObject(
                "select id from rulesystem.rule_system where code = 'ESP'",
                Long.class
        );
        Long contrato = DatosDePrueba.ruleEntity(
                jdbcTemplate, "CONTRACT", "ZC1", "Test Contract",
                LocalDate.of(1900, 1, 1), null
        );
        Long subtipoRelacionado = DatosDePrueba.ruleEntity(
                jdbcTemplate, "CONTRACT_SUBTYPE", "ZS1", "Related Subtype",
                LocalDate.of(1900, 1, 1), null
        );
        DatosDePrueba.ruleEntity(
                jdbcTemplate, "CONTRACT_SUBTYPE", "ZS2", "Unrelated Subtype",
                LocalDate.of(1900, 1, 1), null
        );

        jdbcTemplate.update(
                """
                insert into rulesystem.contract_subtype_relation (
                    rule_system_id, contract_rule_entity_id, subtype_rule_entity_id, start_date, end_date, is_active
                ) values (?,?,?,?,?,?)
                """,
                ruleSystemId,
                contrato,
                subtipoRelacionado,
                LocalDate.of(1900, 1, 1),
                null,
                true
        );
    }
}
