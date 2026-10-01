package com.b4rrhh.payroll_engine.layer;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El candado del backend#159 (ADR-077): las tablas del motor que son ley cuelgan de la capa 4, no
 * de la reglamentación.
 *
 * <p>La lista es la del inventario del issue, y es cerrada a propósito: una tabla de ley nueva —las
 * de IRPF, cuando lleguen— se añade aquí en el mismo commit que la crea. Dos comprobaciones:</p>
 * <ul>
 *   <li><b>el esquema</b>: ninguna conserva {@code rule_system_code}, y su {@code layer_code} apunta
 *       a una capa de nivel 4 por la FK compuesta {@code (layer_code, layer_level)};</li>
 *   <li><b>el código</b>: quien lee o escribe una de estas tablas en {@code src/main/java} —la tabla
 *       detrás de {@code from}, {@code join}, {@code into} o {@code update}; nombrarla en un
 *       comentario o en un mensaje no cuenta— lo hace a través de {@code rulesystem.rule_system_layer}. Comparar la capa con la reglamentación daría
 *       verde con ESP y dejaría a una segunda reglamentación que monte {@code NOM_ESP} sin ley.</li>
 * </ul>
 */
@TestSobreEsquemaReal
class TheLawHangsFromTheLayerOfLevelFourTest {

    static final List<String> LAW_TABLES = List.of(
            "ss_cotizacion_tipos",
            "ss_cotizacion_topes",
            "ss_tarifa_primas_at",
            "ss_desempleo_modalidad_contrato",
            "it_prestacion_tramo");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void noLawTableKeepsTheRuleSystemAndEachPointsToALayerOfLevelFour() {
        for (String table : LAW_TABLES) {
            assertThat(jdbcTemplate.queryForList("""
                    select column_name from information_schema.columns
                     where table_schema = 'payroll_engine' and table_name = ?
                    """, String.class, table))
                    .as(table)
                    .contains("layer_code", "layer_level")
                    .doesNotContain("rule_system_code");

            assertThat(jdbcTemplate.queryForObject("""
                    select count(*) from pg_constraint c
                     where c.conrelid = ('payroll_engine.' || ?)::regclass
                       and c.contype = 'f'
                       and c.confrelid = 'rulesystem.layer'::regclass
                       and pg_get_constraintdef(c.oid) like 'FOREIGN KEY (layer_code, layer_level) REFERENCES rulesystem.layer(code, level)%'
                    """, Integer.class, table))
                    .as(table + ": FK compuesta a la capa").isEqualTo(1);

            assertThat(jdbcTemplate.queryForObject("""
                    select count(*) from payroll_engine.%s t
                      join rulesystem.layer l on l.code = t.layer_code
                     where l.level <> 4
                    """.formatted(table), Integer.class))
                    .as(table + ": filas fuera de la capa 4").isZero();
        }
    }

    @Test
    void whoeverReadsALawTableResolvesItsLayerThroughTheRuleSystemLayers() throws IOException {
        Path sources = Path.of("src", "main", "java");
        List<String> offenders;
        try (Stream<Path> files = Files.walk(sources)) {
            offenders = files
                    .filter(file -> file.toString().endsWith(".java"))
                    .filter(file -> {
                        String source = read(file);
                        boolean readsALawTable = LAW_TABLES.stream()
                                .anyMatch(table -> Pattern.compile(
                                        "(?i)\\b(from|join|into|update)\\s+payroll_engine\\." + table + "\\b")
                                        .matcher(source).find());
                        return readsALawTable && !source.contains("rulesystem.rule_system_layer");
                    })
                    .map(file -> sources.relativize(file).toString())
                    .toList();
        }

        assertThat(offenders)
                .as("leen una tabla de ley sin resolver la capa 4 de la reglamentación")
                .isEmpty();
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(file.toString(), e);
        }
    }
}
