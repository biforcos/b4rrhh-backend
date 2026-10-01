package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nadie lee {@code rulesystem.rule_entity} fuera del adaptador de su puerto, y nadie compara una capa
 * con una reglamentación ({@code backend#157}, paso 2 del camino 5, ADR-077 §4).
 *
 * <h2>Por qué hace falta un candado y no basta con el test del puerto</h2>
 *
 * <p>Mientras todo es de nivel 3, la capa nacional se llama como su reglamentación, y una consulta que
 * haga {@code layer_code = rs.code} da exactamente lo mismo que una que resuelva por nivel. Hasta el
 * {@code backend#158}, que sube {@code COUNTRY} a {@code INT} y esa consulta deja de encontrar países
 * sin que nada falle: devuelve vacío. Doce sitios lo hacían así antes de este issue. El test del
 * puerto prueba que el puerto resuelve bien; este, que nadie más resuelve.
 *
 * <h2>Las dos reglas</h2>
 *
 * <ol>
 *   <li><b>Sólo el adaptador lee la tabla</b>: fuera de {@link #ADAPTADOR}, nadie nombra
 *       {@code rule_entity} detrás de un {@code from}, {@code join}, {@code into} o {@code update},
 *       ni {@code RuleEntityEntity}, ni {@code SpringDataRuleEntityRepository}.
 *   <li><b>Nadie compara una capa con una reglamentación</b>, tampoco el adaptador: ninguna línea
 *       iguala {@code layer_code}/{@code layerCode} con algo que se llame reglamentación
 *       ({@code rule_system_code}, {@code ruleSystemCode}, {@code rs.code}). Ya no existe
 *       {@code rule_entity.rule_system_code}; esto vigila que no se rehaga por {@code join}.
 * </ol>
 *
 * <p>Las excepciones llevan nombre y motivo, y valen en las dos direcciones: una excepción que ya no
 * hace falta también sale roja, para que la lista no se quede con nombres que no protegen nada.
 *
 * <p>Se lee el {@code .java} sin comentarios, como el {@code EveryDatedWriteAnnouncesItselfThroughOnePortTest}.
 * Limitación, dicha: una consulta que llegue a la tabla por un nombre que no sea éste —una vista, un
 * {@code search_path}— no se verá, y entonces este test hay que reescribirlo, no borrarlo.
 */
class NobodyReadsRuleEntityOutsideItsPortTest {

    private static final Path FUENTES = Path.of("src/main/java");

    /** El adaptador del puerto {@code RuleEntityRepository}: la entidad JPA, su repositorio y quien los usa. */
    private static final Set<String> ADAPTADOR = Set.of(
            "com/b4rrhh/rulesystem/infrastructure/persistence/RuleEntityEntity.java",
            "com/b4rrhh/rulesystem/infrastructure/persistence/SpringDataRuleEntityRepository.java",
            "com/b4rrhh/rulesystem/infrastructure/persistence/RuleEntityPersistenceAdapter.java");

    /** Quien lee la tabla sin pasar por el puerto, y por qué. */
    private static final Map<String, String> PUEDEN_LEER_LA_TABLA = new TreeMap<>(Map.of(
            "com/b4rrhh/rulesystem/translation/infrastructure/persistence/RuleEntityTranslationCoverageReadAdapter.java",
            "El informe de cobertura de traducciones cuenta filas, no resuelve por reglamentación: una"
                    + " entidad de INT es una fila aunque la monten tres. Cada fila dice su capa."));

    /** Quien pone una capa al lado de una reglamentación sin resolver, y por qué. */
    private static final Map<String, String> PUEDEN_IGUALAR_CAPA_Y_REGLAMENTACION = new TreeMap<>(Map.of());

    private static final Pattern LEE_LA_TABLA = Pattern.compile(
            "(?i)\\b(?:from|join|into|update)\\s+(?:rulesystem\\.)?rule_entity\\b(?!_)"
                    + "|\\bRuleEntityEntity\\b|\\bSpringDataRuleEntityRepository\\b");

    private static final Pattern IGUALA_CAPA_Y_REGLAMENTACION = Pattern.compile(
            "(?i)\\b(?:layer_code|layerCode)\\b\\s*(?:=|\\bin\\b)[^\\n;]*?(?:rule_?system|\\brs\\.code\\b)"
                    + "|(?:rule_?system_?code|\\brs\\.code\\b)\\s*=[^\\n;]*?\\b(?:layer_code|layerCode)\\b");

    private static final Pattern COMENTARIO = Pattern.compile("/\\*.*?\\*/|^\\s*//[^\\n]*", Pattern.DOTALL | Pattern.MULTILINE);

    @Test
    void onlyTheAdapterOfThePortReadsTheTable() {
        Map<String, String> leen = buscar(LEE_LA_TABLA);
        ADAPTADOR.forEach(leen::remove);

        comprobar(leen, PUEDEN_LEER_LA_TABLA,
                "lee rulesystem.rule_entity sin pasar por RuleEntityRepository",
                "Pídeselo al puerto: (reglamentación, tipo, código) o (reglamentación, tipo). Si de verdad"
                        + " no resuelve —recorre filas—, apúntalo en PUEDEN_LEER_LA_TABLA con su motivo.");

        assertTrue(ADAPTADOR.stream().allMatch(fichero -> Files.exists(FUENTES.resolve(fichero))),
                "El adaptador del puerto se ha movido o renombrado: actualiza ADAPTADOR en un commit que se vea.");
    }

    @Test
    void nobodyComparesALayerWithARuleSystem() {
        comprobar(buscar(IGUALA_CAPA_Y_REGLAMENTACION), PUEDEN_IGUALAR_CAPA_Y_REGLAMENTACION,
                "iguala una capa con una reglamentación",
                "Una reglamentación monta una capa por nivel; cuál, lo dice rulesystem.rule_system_layer"
                        + " con el nivel del tipo (ADR-077 §4). Igualarlas sólo acierta en el nivel 3.");
    }

    private static void comprobar(
            Map<String, String> encontrados,
            Map<String, String> excepciones,
            String falta,
            String remedio
    ) {
        TreeSet<String> sinPermiso = new TreeSet<>(encontrados.keySet());
        sinPermiso.removeAll(excepciones.keySet());

        TreeSet<String> excepcionesSobrantes = new TreeSet<>(excepciones.keySet());
        excepcionesSobrantes.removeAll(encontrados.keySet());

        StringBuilder mensaje = new StringBuilder();
        sinPermiso.forEach(fichero -> mensaje.append("\n  ").append(fichero).append(" ").append(falta)
                .append(": «").append(encontrados.get(fichero)).append("»"));
        if (!sinPermiso.isEmpty()) {
            mensaje.append("\n").append(remedio);
        }
        excepcionesSobrantes.forEach(fichero -> mensaje.append("\n  ").append(fichero)
                .append(" está en las excepciones y ya no lo hace: sácalo de la lista."));

        assertTrue(mensaje.isEmpty(), "backend#157:" + mensaje);
    }

    /** Fichero relativo a {@code src/main/java} → la primera coincidencia, para el mensaje. */
    private static Map<String, String> buscar(Pattern patron) {
        assertTrue(Files.isDirectory(FUENTES), "No encuentro " + FUENTES.toAbsolutePath());

        Map<String, String> encontrados = new TreeMap<>();
        for (Path fichero : ficheros()) {
            String fuente = COMENTARIO.matcher(leer(fichero)).replaceAll("");
            Matcher coincidencia = patron.matcher(fuente);
            if (coincidencia.find()) {
                encontrados.put(FUENTES.relativize(fichero).toString().replace('\\', '/'),
                        coincidencia.group().strip());
            }
        }
        return encontrados;
    }

    private static List<Path> ficheros() {
        try (Stream<Path> todos = Files.walk(FUENTES)) {
            return todos.filter(fichero -> fichero.toString().endsWith(".java")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String leer(Path fichero) {
        try {
            return Files.readString(fichero, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
