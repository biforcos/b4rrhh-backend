package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.support.TestSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * backend#26: borrar un código se lleva sus traducciones. La traducción no es un uso del
 * código —eso es {@code RuleEntityInUseException}, que sigue donde estaba—, es un satélite
 * cuya identidad entera es {@code (rule_entity_id, language_code)}. La cascada va en la DDL
 * (V105) para que valga en cualquier camino de borrado, no sólo en este servicio.
 */
@TestSobreEsquemaReal
class DeleteRuleEntityCascadesTranslationsIntegrationTest {

    private static final String ENTRY_REASON = "EMPLOYEE_PRESENCE_ENTRY_REASON";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DeleteRuleEntityUseCase deleteRuleEntityUseCase;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deletingATranslatedCodeDeletesTheCodeAndItsTranslationWithIt() {
        Long ruleEntityId = jdbcTemplate.queryForObject("""
                select id from rulesystem.rule_entity
                 where rule_system_code = 'ESP' and rule_entity_type_code = ? and code = 'HIRING'
                """, Long.class, ENTRY_REASON);
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name)
                values (?, 'es-ES', 'Contratación')
                """, ruleEntityId);

        deleteRuleEntityUseCase.delete(new DeleteRuleEntityCommand(
                "ESP", ENTRY_REASON, "HIRING", LocalDate.of(1900, 1, 1)));
        // El borrado JPA se vuelca al commit; aquí la transacción es la del test y hay que forzarlo
        // para que la cascada de la base de datos actúe y el JdbcTemplate la vea.
        entityManager.flush();

        assertThat(count("rulesystem.rule_entity", ruleEntityId)).as("el código").isZero();
        assertThat(count("rulesystem.rule_entity_translation", ruleEntityId)).as("su traducción").isZero();
    }

    // El seguro de que la cascada está en la DDL y no sólo en el servicio.
    @Test
    void theTranslationForeignKeyCascadesOnDelete() {
        String deleteAction = jdbcTemplate.queryForObject("""
                select confdeltype from pg_constraint
                 where conname = 'fk_rule_entity_translation_rule_entity'
                """, String.class);

        assertThat(deleteAction).isEqualTo("c");
    }

    private Integer count(String table, Long ruleEntityId) {
        String column = table.endsWith("_translation") ? "rule_entity_id" : "id";
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " = ?", Integer.class, ruleEntityId);
    }
}
