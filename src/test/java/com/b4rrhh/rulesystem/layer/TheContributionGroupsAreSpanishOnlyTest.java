package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * backend#163: los grupos de cotización son de la Seguridad Social española, y la V84 los sembró
 * con un {@code cross join} en las tres reglamentaciones. Desde la V173 viven sólo en la capa
 * {@code ESP}, los once; el tipo sigue en el nivel 3, y cuando FRA o PRT tengan nómina sembrarán
 * el suyo.
 */
@TestSobreEsquemaReal
class TheContributionGroupsAreSpanishOnlyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void theElevenContributionGroupsLiveOnlyInTheSpanishLayer() {
        List<Map<String, Object>> porCapa = jdbcTemplate.queryForList("""
                select layer_code, count(*) as n from rulesystem.rule_entity
                 where rule_entity_type_code = 'GRUPO_COTIZACION'
                 group by layer_code
                """);

        assertThat(porCapa).containsExactly(Map.of("layer_code", "ESP", "n", 11L));
        assertThat(jdbcTemplate.queryForObject(
                "select level from rulesystem.rule_entity_type where code = 'GRUPO_COTIZACION'", Integer.class))
                .isEqualTo(3);
    }
}
