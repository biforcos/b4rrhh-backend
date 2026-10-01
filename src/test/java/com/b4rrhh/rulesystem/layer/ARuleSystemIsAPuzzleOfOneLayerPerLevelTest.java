package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * backend#156, paso 1 del camino 5 (ADR-077): las capas existen y todo está en el nivel 3.
 *
 * <p>Sobre el esquema real, que es el de Flyway: lo que afirma es la forma que dejó la V166, y
 * que el propio esquema impide deshacerla. Las dos guardas que miran otra tabla son triggers
 * diferidos, que saltan al confirmar; como el test no confirma nunca (hace rollback), cada caso
 * que debe fallar fuerza la comprobación con {@code set constraints all immediate}.</p>
 */
@TestSobreEsquemaReal
class ARuleSystemIsAPuzzleOfOneLayerPerLevelTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void thereAreFiveFixedLevels() {
        assertThat(jdbcTemplate.queryForList("select level || ' ' || name from rulesystem.level order by level",
                String.class))
                .containsExactly("1 Común", "2 Internacional", "3 Nacional", "4 Nómina nacional",
                        "5 Nómina de empresa");
    }

    @Test
    void everyRuleSystemMountsExactlyOneLayerPerLevelAndEachLayerIsOfThatLevel() {
        List<Map<String, Object>> broken = jdbcTemplate.queryForList("""
                select rs.code, count(rsl.level) as mounted,
                       count(*) filter (where l.level <> rsl.level) as misleveled
                  from rulesystem.rule_system rs
                  left join rulesystem.rule_system_layer rsl on rsl.rule_system_code = rs.code
                  left join rulesystem.layer l on l.code = rsl.layer_code
                 group by rs.code
                having count(distinct rsl.level) <> 5
                    or count(rsl.level) <> 5
                    or count(*) filter (where l.level <> rsl.level) > 0
                """);

        assertThat(broken).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from rulesystem.rule_system", Integer.class))
                .isPositive();
    }

    @Test
    void eachSeedRuleSystemSharesComAndIntAndBringsItsOwnThree() {
        List<String> puzzles = jdbcTemplate.queryForList("""
                select rule_system_code || ' = ' || string_agg(layer_code, ', ' order by level)
                  from rulesystem.rule_system_layer
                 where rule_system_code in ('ESP', 'FRA', 'PRT')
                 group by rule_system_code
                 order by rule_system_code
                """, String.class);

        assertThat(puzzles).containsExactly(
                "ESP = COM, INT, ESP, NOM_ESP, NOM_ESP_EMP",
                "FRA = COM, INT, FRA, NOM_FRA, NOM_FRA_EMP",
                "PRT = COM, INT, PRT, NOM_PRT, NOM_PRT_EMP");
    }

    @Test
    void everyEntityLivesInALayerOfTheLevelOfItsType() {
        List<String> misplaced = jdbcTemplate.queryForList("""
                select re.layer_code || '/' || re.rule_entity_type_code || '/' || re.code
                  from rulesystem.rule_entity re
                  join rulesystem.layer l            on l.code = re.layer_code
                  join rulesystem.rule_entity_type t on t.code = re.rule_entity_type_code
                 where l.level <> t.level
                """, String.class);

        assertThat(misplaced).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from rulesystem.rule_entity", Integer.class))
                .isPositive();
    }

    // El paso 1 decia «todo en el nivel 3». El paso 3 (backend#158) sube los tipos que son iguales
    // en cualquier pais a COM e INT —cuales, lo dice EachRaisedTypeResolvesTheSameFromEveryRuleSystemTest—,
    // y las capas de nomina, 4 y 5, siguen vacias hasta que el motor se mude (backend#159), que es
    // quien tiene que cambiar este test.
    @Test
    void untilTheEngineMovesOnlyTheLevelsUpToTheNationalOneHoldEntities() {
        assertThat(jdbcTemplate.queryForList(
                "select distinct level from rulesystem.rule_entity_type", Integer.class))
                .isSubsetOf(1, 2, 3);
        assertThat(jdbcTemplate.queryForList("""
                select distinct l.level
                  from rulesystem.rule_entity re
                  join rulesystem.layer l on l.code = re.layer_code
                """, Integer.class))
                .isSubsetOf(1, 2, 3);
    }

    @Test
    void anEntityCannotLiveInALayerOfAnotherLevel() {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity (layer_code, rule_entity_type_code, code, name, start_date)
                values ('COM', 'CONTACT_TYPE', 'ZZ_PROBE', 'Sonda', date '2020-01-01')
                """);

        assertThatThrownBy(() -> jdbcTemplate.execute("set constraints all immediate"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("COM/CONTACT_TYPE/ZZ_PROBE");
    }

    @Test
    void aTypeCannotChangeLevelLeavingItsEntitiesBehind() {
        jdbcTemplate.update("update rulesystem.rule_entity_type set level = 2 where code = 'CONTRACT'");

        assertThatThrownBy(() -> jdbcTemplate.execute("set constraints all immediate"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("CONTRACT");
    }

    @Test
    void aRuleSystemCannotBeLeftWithoutOneOfItsLayers() {
        jdbcTemplate.update("delete from rulesystem.rule_system_layer where rule_system_code = 'FRA' and level = 5");

        assertThatThrownBy(() -> jdbcTemplate.execute("set constraints all immediate"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("FRA");
    }

    @Test
    void aRuleSystemCannotBeCreatedWithoutItsLayers() {
        jdbcTemplate.update("insert into rulesystem.rule_system (code, name, country_code) values ('ZZZ', 'Sonda', 'ZZZ')");

        assertThatThrownBy(() -> jdbcTemplate.execute("set constraints all immediate"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ZZZ");
    }

    @Test
    void aLayerMountedAtALevelMustBeOfThatLevel() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update rulesystem.rule_system_layer set layer_code = 'INT' where rule_system_code = 'FRA' and level = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theSameCodeInTwoLayersDoesNotClash() {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity (layer_code, rule_entity_type_code, code, name, start_date)
                values ('ESP', 'CONTACT_TYPE', 'ZZ_PROBE', 'Sonda', date '2020-01-01'),
                       ('FRA', 'CONTACT_TYPE', 'ZZ_PROBE', 'Sonde', date '2020-01-01')
                """);
        jdbcTemplate.execute("set constraints all immediate");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_entity where code = 'ZZ_PROBE'", Integer.class))
                .isEqualTo(2);
    }
}
