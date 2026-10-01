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
 * es el que lo distingue: sube {@code COUNTRY} al nivel 2 dentro del test, con sus países en
 * {@code INT}, y el puerto tiene que encontrarlos desde cualquier reglamentación. Antes de mirar
 * el puerto, el esquema confirma que ese estado es válido ({@code set constraints all immediate}):
 * el test no se inventa un mundo que la base no admitiría.</p>
 */
@TestSobreEsquemaReal
class TheRuleEntityPortResolvesByTheLevelOfTheTypeTest {

    @Autowired
    private RuleEntityRepository ruleEntityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aTypeOfLevelThreeResolvesInTheNationalLayerOfTheRuleSystem() {
        RuleEntity spain = ruleEntityRepository.findByBusinessKey("ESP", "COUNTRY", "ESP").orElseThrow();

        assertThat(spain.getRuleSystemCode()).isEqualTo("ESP");
        assertThat(spain.getLayerCode()).isEqualTo("ESP");
        assertThat(spain.getLevel()).isEqualTo(3);
    }

    @Test
    void aTypeRaisedToLevelTwoResolvesInIntFromEveryRuleSystem() {
        raiseCountryToInt();

        for (String ruleSystemCode : List.of("ESP", "FRA", "PRT")) {
            RuleEntity spain = ruleEntityRepository.findByBusinessKey(ruleSystemCode, "COUNTRY", "ESP")
                    .orElseThrow(() -> new AssertionError("COUNTRY/ESP no resuelve desde " + ruleSystemCode));

            assertThat(spain.getRuleSystemCode()).isEqualTo(ruleSystemCode);
            assertThat(spain.getLayerCode()).isEqualTo("INT");
            assertThat(spain.getLevel()).isEqualTo(2);
            assertThat(ruleEntityRepository.findApplicableByBusinessKey(
                    ruleSystemCode, "COUNTRY", "ESP", LocalDate.of(2026, 1, 1)))
                    .map(RuleEntity::getId).contains(spain.getId());
        }

        List<RuleEntity> countriesSeenFromFrance = ruleEntityRepository.findByFilters("FRA", "COUNTRY", null, null, null);
        assertThat(countriesSeenFromFrance).hasSize(10)
                .allSatisfy(country -> {
                    assertThat(country.getLayerCode()).isEqualTo("INT");
                    assertThat(country.getRuleSystemCode()).isEqualTo("FRA");
                });
    }

    @Test
    void anotherTypeOfLevelThreeStillResolvesInItsNationalLayerWhenCountryMoves() {
        raiseCountryToInt();

        assertThat(ruleEntityRepository.findByFilters("FRA", "CONTACT_TYPE", null, null, null))
                .isNotEmpty()
                .allSatisfy(type -> assertThat(type.getLayerCode()).isEqualTo("FRA"));
    }

    /** Lo que hará el backend#158 con {@code COUNTRY}, en pequeño: un tipo, sus diez países, sin traducir. */
    private void raiseCountryToInt() {
        jdbcTemplate.update("update rulesystem.rule_entity_type set level = 2 where code = 'COUNTRY'");
        jdbcTemplate.update("""
                delete from rulesystem.rule_entity
                 where rule_entity_type_code = 'COUNTRY' and layer_code in ('FRA', 'PRT')
                """);
        jdbcTemplate.update("""
                update rulesystem.rule_entity set layer_code = 'INT'
                 where rule_entity_type_code = 'COUNTRY' and layer_code = 'ESP'
                """);
        jdbcTemplate.execute("set constraints all immediate");
    }
}
