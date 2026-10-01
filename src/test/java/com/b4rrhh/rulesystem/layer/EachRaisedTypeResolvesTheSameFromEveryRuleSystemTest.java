package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * backend#158, paso 3 del camino 5 (ADR-077): cada tipo que sube de nivel vive en una sola capa.
 * Desde ESP, FRA y PRT resuelve la misma fila —el mismo {@code id}— en la capa de destino, con el
 * mismo nombre traducido; y en ninguna capa nacional queda una copia. Una fila por tipo que sube,
 * añadida en el mismo commit que su migración.
 */
@TestSobreEsquemaReal
class EachRaisedTypeResolvesTheSameFromEveryRuleSystemTest {

    private static final List<String> RULE_SYSTEMS = List.of("ESP", "FRA", "PRT");

    @Autowired
    private RuleEntityRepository ruleEntityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest(name = "{0} vive en {1}")
    @CsvSource({
            "COUNTRY, INT, 2, ESP, España"
    })
    void theTypeResolvesToOneRowInItsLayerFromEveryRuleSystem(
            String type, String layer, int level, String code, String spanishName) {
        Map<String, Long> idSeenFrom = RULE_SYSTEMS.stream().collect(Collectors.toMap(
                ruleSystem -> ruleSystem,
                ruleSystem -> {
                    RuleEntity entity = ruleEntityRepository.findByBusinessKey(ruleSystem, type, code)
                            .orElseThrow(() -> new AssertionError(type + "/" + code + " no resuelve desde " + ruleSystem));
                    assertThat(entity.getLayerCode()).isEqualTo(layer);
                    assertThat(entity.getLevel()).isEqualTo(level);
                    return entity.getId();
                }));
        assertThat(idSeenFrom.values().stream().distinct()).as("el mismo id desde las tres").hasSize(1);

        assertThat(jdbcTemplate.queryForObject("""
                select tr.name from rulesystem.rule_entity_translation tr
                 where tr.rule_entity_id = ? and tr.language_code = 'es-ES'
                """, String.class, idSeenFrom.get("ESP"))).isEqualTo(spanishName);

        assertThat(jdbcTemplate.queryForObject("""
                select level from rulesystem.rule_entity_type where code = ?
                """, Integer.class, type)).isEqualTo(level);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from rulesystem.rule_entity re
                  join rulesystem.layer l on l.code = re.layer_code
                 where re.rule_entity_type_code = ? and l.level <> ?
                """, Integer.class, type, level)).as("copias fuera de " + layer).isZero();
    }

    @ParameterizedTest(name = "{0}: {1} filas en {2}")
    @CsvSource({
            "COUNTRY, 249, INT"
    })
    void theLayerHoldsTheWholeCatalogTranslated(String type, int rows, String layer) {
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from rulesystem.rule_entity where rule_entity_type_code = ? and layer_code = ?
                """, Integer.class, type, layer)).isEqualTo(rows);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from rulesystem.rule_entity re
                 where re.rule_entity_type_code = ? and re.layer_code = ?
                   and not exists (select 1 from rulesystem.rule_entity_translation tr
                                    where tr.rule_entity_id = re.id and tr.language_code = 'es-ES')
                """, Integer.class, type, layer)).as("filas sin castellano").isZero();
        assertThat(ruleEntityRepository.findByFilters("PRT", type, null, null, null)).hasSize(rows);
    }
}
