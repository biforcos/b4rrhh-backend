package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * backend#157, paso 2 del camino 5 (ADR-077 §4): el puerto resuelve {@code (reglamentación, tipo,
 * código)} por el nivel del tipo, la capa que esa reglamentación monta en ese nivel y la entidad
 * de esa capa. No sube por ninguna cadena.
 *
 * <p>Hoy todo es de nivel 3 y la capa nacional se llama como su reglamentación, así que un puerto
 * que comparase capa con reglamentación daría verde con los datos de la semilla. El segundo caso
 * es el que lo distingue: sube un tipo al nivel 2 dentro del test, con sus entidades en
 * {@code INT}, y el puerto tiene que encontrarlas desde cualquier reglamentación. Antes de mirar
 * el puerto, el esquema confirma que ese estado es válido ({@code set constraints all immediate}):
 * el test no se inventa un mundo que la base no admitiría.</p>
 *
 * <p>Cuando se escribió, el tipo que subía era {@code COUNTRY}; el backend#158 lo subió de verdad
 * (V168), y con él {@code CONTACT_TYPE}. Los casos usan ahora tipos que se quedan en el nivel 3:
 * los motivos de baja y los de alta. Los de alta sustituyeron a los grupos de cotización cuando el
 * backend#163 los dejó sólo en ESP (V173): el caso necesita un tipo de nivel 3 con entidades en
 * FRA. Lo que comprueba el test no ha cambiado.</p>
 */
@TestSobreEsquemaReal
class TheRuleEntityPortResolvesByTheLevelOfTheTypeTest {

    @Autowired
    private RuleEntityRepository ruleEntityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aTypeOfLevelThreeResolvesInTheNationalLayerOfTheRuleSystem() {
        RuleEntity termination = ruleEntityRepository
                .findByBusinessKey("ESP", "EMPLOYEE_PRESENCE_EXIT_REASON", "TERMINATION").orElseThrow();

        assertThat(termination.getRuleSystemCode()).isEqualTo("ESP");
        assertThat(termination.getLayerCode()).isEqualTo("ESP");
        assertThat(termination.getLevel()).isEqualTo(3);
    }

    @Test
    void aTypeRaisedToLevelTwoResolvesInIntFromEveryRuleSystem() {
        raiseExitReasonToInt();

        for (String ruleSystemCode : List.of("ESP", "FRA", "PRT")) {
            RuleEntity termination = ruleEntityRepository
                    .findByBusinessKey(ruleSystemCode, "EMPLOYEE_PRESENCE_EXIT_REASON", "TERMINATION")
                    .orElseThrow(() -> new AssertionError("TERMINATION no resuelve desde " + ruleSystemCode));

            assertThat(termination.getRuleSystemCode()).isEqualTo(ruleSystemCode);
            assertThat(termination.getLayerCode()).isEqualTo("INT");
            assertThat(termination.getLevel()).isEqualTo(2);
            assertThat(ruleEntityRepository.findApplicableByBusinessKey(
                    ruleSystemCode, "EMPLOYEE_PRESENCE_EXIT_REASON", "TERMINATION", LocalDate.of(2026, 1, 1)))
                    .map(RuleEntity::getId).contains(termination.getId());
        }

        List<RuleEntity> reasonsSeenFromFrance =
                ruleEntityRepository.findByFilters("FRA", "EMPLOYEE_PRESENCE_EXIT_REASON", null, null, null);
        assertThat(reasonsSeenFromFrance).hasSize(3)
                .allSatisfy(reason -> {
                    assertThat(reason.getLayerCode()).isEqualTo("INT");
                    assertThat(reason.getRuleSystemCode()).isEqualTo("FRA");
                });
    }

    @Test
    void anotherTypeOfLevelThreeStillResolvesInItsNationalLayerWhenOneMoves() {
        raiseExitReasonToInt();

        assertThat(ruleEntityRepository.findByFilters("FRA", "EMPLOYEE_PRESENCE_ENTRY_REASON", null, null, null))
                .isNotEmpty()
                .allSatisfy(reason -> assertThat(reason.getLayerCode()).isEqualTo("FRA"));
    }

    /** Una subida en pequeño, a mano: un tipo, sus tres motivos, sin traducir. */
    private void raiseExitReasonToInt() {
        jdbcTemplate.update("update rulesystem.rule_entity_type set level = 2 where code = 'EMPLOYEE_PRESENCE_EXIT_REASON'");
        jdbcTemplate.update("""
                delete from rulesystem.rule_entity
                 where rule_entity_type_code = 'EMPLOYEE_PRESENCE_EXIT_REASON' and layer_code in ('FRA', 'PRT')
                """);
        jdbcTemplate.update("""
                update rulesystem.rule_entity set layer_code = 'INT'
                 where rule_entity_type_code = 'EMPLOYEE_PRESENCE_EXIT_REASON' and layer_code = 'ESP'
                """);
        jdbcTemplate.execute("set constraints all immediate");
    }
}
