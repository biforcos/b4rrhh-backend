package com.b4rrhh.rulesystem;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * backend#152: los nombres de semilla que estaban en inglés sin ser vocabulario —el de la
 * reglamentación ESP y los de las empresas, que son nombres propios— se sembraron así. No se
 * traducen: se renombran, en la V165.
 */
@TestSobreEsquemaReal
class SeedNamesInSpanishFlywayIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void theRuleSystemsAreNamedInSpanish() {
        assertThat(jdbcTemplate.queryForList(
                "select name from rulesystem.rule_system where code in ('ESP', 'FRA', 'PRT') order by code",
                String.class))
                .containsExactly("Administración de personal · España", "Francia", "Portugal");
    }

    @Test
    void theSeedCompaniesAreNamedInSpanish() {
        List<String> names = jdbcTemplate.queryForList("""
                select distinct name
                  from rulesystem.rule_entity
                 where rule_entity_type_code = 'COMPANY'
                   and code in ('ES01', 'ES02', 'FR01', 'PT01')
                 order by name
                """, String.class);

        assertThat(names).containsExactly(
                "Empresa España 01", "Empresa España 02", "Empresa Francia 01", "Empresa Portugal 01");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from rulesystem.rule_entity
                 where rule_entity_type_code = 'COMPANY' and name like '%Company%'
                """, Integer.class)).isZero();
    }
}
