package com.b4rrhh.rulesystem.translation;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La tabla gemela de traducciones de los tipos sobre el esquema real (backend#152).
 *
 * Un tipo no es una {@code rule_entity}, así que su nombre no cabe en
 * {@code rule_entity_translation}: la gemela cuelga del código del tipo, con el mismo
 * formato de idioma. La V164 siembra el castellano de todos los tipos que hay.
 */
@TestSobreEsquemaReal
class RuleEntityTypeTranslationFlywayIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyTypeHasItsNameInSpanish() {
        List<String> untranslated = jdbcTemplate.queryForList("""
                select t.code
                  from rulesystem.rule_entity_type t
                 where not exists (
                        select 1
                          from rulesystem.rule_entity_type_translation tr
                         where tr.rule_entity_type_code = t.code
                           and tr.language_code = 'es-ES')
                """, String.class);

        assertThat(untranslated).isEmpty();
    }

    // Un caso por invocación: la primera inserción rechazada aborta la transacción del test.
    @ParameterizedTest
    @ValueSource(strings = {"es_ES", "ES", "es-es", "spa"})
    void theSchemaRejectsALanguageCodeThatIsNotShortBcp47(String badLanguage) {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into rulesystem.rule_entity_type_translation (rule_entity_type_code, language_code, name)
                values ('COUNTRY', ?, 'País')
                """, badLanguage))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Un tipo que se retira se lleva sus traducciones: el precedente de la casa es retirar un
    // tipo con un delete en una migración (ADR-054 §1), y la gemela no debe impedirlo.
    @Test
    void retiringATypeTakesItsTranslationsWithIt() {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_type (code, name, literal_class, maintenance_mode, group_code, level)
                values ('BACKEND_152_PROBE', 'Probe', 'DOMAIN_VOCABULARY', 'MAINTAINED', 'ORGANIZATION', 3)
                """);
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_type_translation (rule_entity_type_code, language_code, name)
                values ('BACKEND_152_PROBE', 'es-ES', 'Sonda')
                """);

        jdbcTemplate.update("delete from rulesystem.rule_entity_type where code = 'BACKEND_152_PROBE'");

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from rulesystem.rule_entity_type_translation
                 where rule_entity_type_code = 'BACKEND_152_PROBE'
                """, Integer.class)).isZero();
    }
}
