package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * backend#158: subir un tipo de nivel sólo es legítimo si sus copias nacionales son la misma cosa
 * escrita tres veces. Si divergen, la subida falla y dice en qué: eso es información, no un
 * obstáculo. La función es la que usan las migraciones de la V167 en adelante; aquí se ejerce con un
 * tipo de sonda dentro de la transacción del test, que se deshace sola.
 */
@TestSobreEsquemaReal
class RaisingATypeRefusesCopiesThatDifferTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void aProbeTypeCopiedInTheThreeNationalLayers() {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_type (code, name, literal_class, maintenance_mode, group_code, level)
                values ('ZZ_PROBE', 'Probe', 'DOMAIN_VOCABULARY', 'MAINTAINED', 'ORGANIZATION', 3)
                """);
        for (String layer : new String[] {"ESP", "FRA", "PRT"}) {
            for (String code : new String[] {"ONE", "TWO"}) {
                jdbcTemplate.update("""
                        insert into rulesystem.rule_entity (layer_code, rule_entity_type_code, code, name, start_date)
                        values (?, 'ZZ_PROBE', ?, ?, date '1900-01-01')
                        """, layer, code, "Probe " + code);
                jdbcTemplate.update("""
                        insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name)
                        select id, 'es-ES', 'Sonda ' || code from rulesystem.rule_entity
                         where layer_code = ? and rule_entity_type_code = 'ZZ_PROBE' and code = ?
                        """, layer, code);
            }
        }
    }

    @Test
    void identicalCopiesBecomeOneRowInTheTargetLayerWithItsTranslation() {
        jdbcTemplate.execute("select rulesystem.raise_rule_entity_type('ZZ_PROBE', 'COM')");
        jdbcTemplate.execute("set constraints all immediate");

        assertThat(jdbcTemplate.queryForList("""
                select re.layer_code || '/' || re.code || '/' || tr.name
                  from rulesystem.rule_entity re
                  join rulesystem.rule_entity_translation tr on tr.rule_entity_id = re.id
                 where re.rule_entity_type_code = 'ZZ_PROBE'
                 order by re.code
                """, String.class)).containsExactly("COM/ONE/Sonda ONE", "COM/TWO/Sonda TWO");
        assertThat(jdbcTemplate.queryForObject(
                "select level from rulesystem.rule_entity_type where code = 'ZZ_PROBE'", Integer.class)).isEqualTo(1);
    }

    @Test
    void aNameThatDiffersInOneCountryStopsTheRaiseAndSaysWhere() {
        jdbcTemplate.update("""
                update rulesystem.rule_entity set name = 'Sonde'
                 where layer_code = 'FRA' and rule_entity_type_code = 'ZZ_PROBE' and code = 'TWO'
                """);

        assertThat(raiseFailure())
                .contains("ZZ_PROBE")
                .contains("TWO")
                .contains("name")
                .contains("FRA")
                .doesNotContain("ONE");
    }

    @Test
    void aTranslationThatDiffersStopsTheRaise() {
        jdbcTemplate.update("""
                update rulesystem.rule_entity_translation set name = 'Sonda distinta'
                 where rule_entity_id = (select id from rulesystem.rule_entity
                                          where layer_code = 'PRT' and rule_entity_type_code = 'ZZ_PROBE' and code = 'ONE')
                """);

        assertThat(raiseFailure()).contains("ONE").contains("translations").contains("PRT");
    }

    @Test
    void aCopyMissingInOneCountryStopsTheRaise() {
        jdbcTemplate.update("""
                delete from rulesystem.rule_entity
                 where layer_code = 'ESP' and rule_entity_type_code = 'ZZ_PROBE' and code = 'ONE'
                """);

        assertThat(raiseFailure()).contains("ONE").contains("ESP");
    }

    @Test
    void aRowThatPointsToACopyIsRepointedAndNotCascadedAway() {
        jdbcTemplate.update("""
                insert into rulesystem.work_center_contact
                    (work_center_rule_entity_id, contact_number, contact_type_code, contact_value)
                select id, 1, 'EMAIL', 'probe@example.com' from rulesystem.rule_entity
                 where layer_code = 'FRA' and rule_entity_type_code = 'ZZ_PROBE' and code = 'ONE'
                """);

        jdbcTemplate.execute("select rulesystem.raise_rule_entity_type('ZZ_PROBE', 'COM')");

        assertThat(jdbcTemplate.queryForObject("""
                select re.layer_code || '/' || re.code from rulesystem.work_center_contact c
                  join rulesystem.rule_entity re on re.id = c.work_center_rule_entity_id
                 where c.contact_value = 'probe@example.com'
                """, String.class)).isEqualTo("COM/ONE");
    }

    private String raiseFailure() {
        DataAccessException failure = catchThrowableOfType(DataAccessException.class,
                () -> jdbcTemplate.execute("select rulesystem.raise_rule_entity_type('ZZ_PROBE', 'COM')"));
        assertThat(failure).as("la subida tenía que fallar").isNotNull();
        return failure.getMostSpecificCause().getMessage();
    }
}
