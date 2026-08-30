package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las cuatro guardias del metamodelo de extensiones (ADR-053 §6, backend#33), sobre el
 * esquema real y con la disciplina de {@code EveryCatalogColumnIsDeclaredOrExemptedTest}
 * (backend#29): se lee lo que hay —{@code rule_entity_extension} y {@code pg_constraint}—,
 * no lo que alguien recuerda que había.
 *
 * <ol>
 *   <li>Cada {@code table_name} declarada existe y cuelga de {@code rule_entity(id)}.</li>
 *   <li>El inverso: cada tabla que cuelga de {@code rule_entity(id)} está declarada o
 *       exenta con motivo. Es lo que impide que un satélite nuevo entre sin verse.</li>
 *   <li>Toda extensión {@code required} tiene fila para cada raíz de su tipo.</li>
 *   <li>Toda tabla declarada cae en cascada. No compara contra ninguna columna —no la
 *       hay (ADR-053 §3)—: lo afirma.</li>
 * </ol>
 */
@TestSobreEsquemaReal
class EveryRuleEntityExtensionIsDeclaredAndEnforcedTest {

    /**
     * Satélites que cuelgan de {@code rule_entity(id)} pero NO son extensión de un tipo
     * concreto, con el motivo. Una exención sin motivo no vale.
     */
    private static final Map<String, String> UNIVERSAL_SATELLITES = Map.of(
            "rulesystem.rule_entity_translation",
            "cuelga de cualquier raíz, sea del tipo que sea (ADR-052 §1); declararla exigiría una fila "
                    + "por tipo y la ausencia dejaría de significar «sólo raíz» (ADR-053 §2). "
                    + "Su cascada se afirma en la guardia 4 igual que la de las declaradas."
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Guardia 1: caza declaraciones caducadas — una tabla renombrada o borrada, o una que
    // perdió su clave ajena, deja de ser una extensión por mucho que la declaración lo diga.
    @Test
    void everyDeclaredTableExistsAndHangsFromRuleEntity() {
        Set<String> declared = declaredTables();
        assertThat(declared).as("la declaración está sembrada").isNotEmpty();

        Set<String> stale = new TreeSet<>(declared);
        stale.removeAll(satelliteTables());

        assertThat(stale)
                .withFailMessage("""
                        Estas tablas declaradas en rule_entity_extension no existen o no tienen \
                        clave ajena a rule_entity(id): %s

                        O la tabla cambió y la declaración no la siguió, o la declaración se \
                        sembró mal. Se arregla la declaración, no se borra la guardia.
                        """, stale)
                .isEmpty();
    }

    // Guardia 2: el inverso. Un satélite nuevo que cuelgue de la raíz aparece aquí solo.
    @Test
    void everyTableHangingFromRuleEntityIsDeclaredOrExempted() {
        assertThat(undeclared())
                .withFailMessage("""
                        Estas tablas cuelgan de rule_entity(id) y no están declaradas en \
                        rule_entity_extension ni exentas: %s

                        Si son extensión de un tipo, se declaran en una migración (una fila por \
                        tipo y extensión, ADR-053 §2). Si cuelgan de cualquier raíz sin importar \
                        el tipo, se añaden a UNIVERSAL_SATELLITES en este test con el motivo \
                        escrito.
                        """, undeclared())
                .isEmpty();
    }

    // La prueba de la guardia 2: una tabla nueva con clave ajena a rule_entity(id) hace
    // fallar la suite sin que nadie la apunte en ningún sitio. El DDL va dentro de la
    // transacción del test y se deshace con ella.
    @Test
    void aNewSatelliteTableShowsUpByItself() {
        jdbcTemplate.execute("""
                create table rulesystem.zz_probe_satellite (
                    rule_entity_id bigint not null references rulesystem.rule_entity(id)
                )""");

        assertThat(undeclared()).containsExactly("rulesystem.zz_probe_satellite");
    }

    // Guardia 3: la que el orElse de ListCompaniesService lleva tapando desde V36. Una
    // extensión required sin fila no rompe la aplicación: le hace enseñar otra cosa parecida.
    @Test
    void everyRequiredExtensionHasARowForEachRootOfItsType() {
        List<Map<String, Object>> required = jdbcTemplate.queryForList("""
                select rule_entity_type_code, extension_code, table_name
                  from rulesystem.rule_entity_extension
                 where required
                 order by rule_entity_type_code, extension_code
                """);
        assertThat(required).as("hay extensiones required declaradas").isNotEmpty();

        Map<String, Long> rootsWithoutExtension = new TreeMap<>();
        for (Map<String, Object> extension : required) {
            String typeCode = (String) extension.get("rule_entity_type_code");
            String table = (String) extension.get("table_name");
            String fkColumn = foreignKeyColumnToRuleEntity(table);
            Long missing = jdbcTemplate.queryForObject("""
                    select count(*) from rulesystem.rule_entity re
                     where re.rule_entity_type_code = ?
                       and not exists (select 1 from %s x where x.%s = re.id)
                    """.formatted(table, fkColumn), Long.class, typeCode);
            if (missing != null && missing > 0) {
                rootsWithoutExtension.put(typeCode + " sin " + extension.get("extension_code"), missing);
            }
        }

        assertThat(rootsWithoutExtension)
                .withFailMessage("""
                        Raíces sin su extensión obligatoria (tipo sin extensión -> cuántas): %s

                        No se arregla a ciegas ni se relaja la guardia: o se siembran las filas \
                        que faltan, o el tipo no debería tener la extensión como required. Es \
                        decisión de modelo (ADR-053, backend#33).
                        """, rootsWithoutExtension)
                .isEmpty();
    }

    // Guardia 4: habría hecho fallar la V104 en su propio commit, en vez de esperar a que
    // alguien borrara un código traducido (backend#26). Vale también para los satélites
    // universales: exentos de declararse, no de caer en cascada.
    @Test
    void everyDeclaredTableCascadesOnDelete() {
        Set<String> tables = declaredTables();
        tables.addAll(UNIVERSAL_SATELLITES.keySet());

        Map<String, String> nonCascading = new TreeMap<>();
        for (String table : tables) {
            jdbcTemplate.queryForList("""
                    select conname, confdeltype from pg_constraint
                     where contype = 'f'
                       and confrelid = 'rulesystem.rule_entity'::regclass
                       and conrelid = ?::regclass
                    """, table).forEach(fk -> {
                if (!"c".equals(String.valueOf(fk.get("confdeltype")))) {
                    nonCascading.put(table + "." + fk.get("conname"), String.valueOf(fk.get("confdeltype")));
                }
            });
        }

        assertThat(nonCascading)
                .withFailMessage("""
                        Estas claves ajenas a rule_entity(id) no caen en cascada (c=cascade, \
                        a=no action, r=restrict): %s

                        Una extensión es algo poseído por la raíz y siempre cae en cascada, sin \
                        excepciones (ADR-053 §3). Si no puede caer en cascada, no es una \
                        extensión: es un uso, y no pertenece a rule_entity_extension.
                        """, nonCascading)
                .isEmpty();
    }

    // Una exención de una tabla que ya no existe, o que además está declarada, es ruido
    // que acaba tapando un hueco real. La misma higiene que en backend#29.
    @Test
    void exemptionsPointToRealSatellitesAndDoNotOverlapDeclarations() {
        Set<String> satellites = satelliteTables();
        Set<String> declared = declaredTables();

        Map<String, String> stale = new TreeMap<>();
        UNIVERSAL_SATELLITES.forEach((table, reason) -> {
            if (!satellites.contains(table)) {
                stale.put(table, "ya no cuelga de rule_entity(id): sobra la exención");
            } else if (declared.contains(table)) {
                stale.put(table, "está declarada en rule_entity_extension: sobra la exención");
            }
            if (reason == null || reason.isBlank()) {
                stale.put(table, "exención sin motivo");
            }
        });

        assertThat(stale).as("exenciones caducadas").isEmpty();
    }

    private Set<String> undeclared() {
        Set<String> undeclared = satelliteTables();
        undeclared.removeAll(declaredTables());
        undeclared.removeAll(UNIVERSAL_SATELLITES.keySet());
        return undeclared;
    }

    private Set<String> declaredTables() {
        return new TreeSet<>(jdbcTemplate.queryForList(
                "select distinct table_name from rulesystem.rule_entity_extension", String.class));
    }

    /** Toda tabla con clave ajena a {@code rule_entity(id)}, con esquema, según Postgres. */
    private Set<String> satelliteTables() {
        return new TreeSet<>(jdbcTemplate.queryForList("""
                select distinct n.nspname || '.' || cl.relname
                  from pg_constraint c
                  join pg_class cl on cl.oid = c.conrelid
                  join pg_namespace n on n.oid = cl.relnamespace
                 where c.contype = 'f'
                   and c.confrelid = 'rulesystem.rule_entity'::regclass
                """, String.class));
    }

    private String foreignKeyColumnToRuleEntity(String table) {
        List<String> columns = jdbcTemplate.queryForList("""
                select a.attname
                  from pg_constraint c
                  join pg_attribute a on a.attrelid = c.conrelid and a.attnum = any (c.conkey)
                 where c.contype = 'f'
                   and c.confrelid = 'rulesystem.rule_entity'::regclass
                   and c.conrelid = ?::regclass
                """, String.class, table);
        assertThat(columns)
                .as("una extensión 1:1 required cuelga de la raíz por una única columna (%s)", table)
                .hasSize(1);
        return columns.get(0);
    }
}
