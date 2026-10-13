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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ningún caso de uso escribe {@code rule_entity} recibiendo una reglamentación sin comprobar antes
 * que la capa de la entidad es la suya, la nacional (backend#166, ADR-077).
 *
 * <h2>Por qué</h2>
 *
 * <p>Desde una reglamentación se ven las entidades de todas las capas que monta: desde ESP, un país
 * sale de {@code INT}. El puerto resuelve igual para leer que para escribir, así que un
 * {@code PUT /rule-entities/ESP/COUNTRY/FRA/…} corregía la fila de INT, que ven FRA y PRT, y un
 * {@code DELETE} la borraba contando sólo las referencias de ESP. La pantalla ya no lo ofrecía
 * (frontend#131); el contrato sí.
 *
 * <h2>La regla</h2>
 *
 * <p>Un fichero que guarda o borra por un {@code RuleEntityRepository} llama a
 * {@code NationalLayerGuard}. Las excepciones llevan nombre y motivo, y valen en las dos
 * direcciones: una que ya no escribe también sale roja.
 *
 * <p>Se lee el {@code .java} sin comentarios. Limitación, dicha: el test comprueba que la guarda se
 * llama, no que se llame antes de escribir ni con la entidad que se escribe. Eso lo dicen los tests
 * de extremo a extremo de cada verbo.
 */
class EveryRuleSystemWriteOfARuleEntityChecksItsLayerTest {

    private static final Path FUENTES = Path.of("src/main/java");

    private static final String PUERTO = "com/b4rrhh/rulesystem/domain/port/RuleEntityRepository.java";

    /** Quien escribe sin pasar por la guarda, y por qué. */
    private static final Map<String, String> ESCRIBEN_POR_CAPA = new TreeMap<>(Map.of(
            "com/b4rrhh/rulesystem/application/usecase/CreateLayerRuleEntityService.java",
            "Escribe por capa (/layers/…): no recibe reglamentación, y comprueba que el tipo es del nivel de la capa.",
            "com/b4rrhh/rulesystem/application/usecase/CorrectLayerRuleEntityService.java",
            "Escribe por capa (/layers/…): la entidad se busca en la capa pedida.",
            "com/b4rrhh/rulesystem/application/usecase/CloseLayerRuleEntityService.java",
            "Escribe por capa (/layers/…): la entidad se busca en la capa pedida.",
            "com/b4rrhh/rulesystem/application/usecase/DeleteLayerRuleEntityService.java",
            "Escribe por capa (/layers/…): cuenta las referencias de todas las reglamentaciones que la montan."));

    private static final Pattern CAMPO_DEL_PUERTO = Pattern.compile("\\bRuleEntityRepository\\s+(\\w+)\\s*[;,)=]");

    private static final String ESCRITURAS = "save|deleteByBusinessKeyAndStartDate|deleteInLayerByBusinessKeyAndStartDate";

    private static final Pattern LLAMA_A_LA_GUARDA = Pattern.compile("\\bNationalLayerGuard\\b");

    private static final Pattern COMENTARIO = Pattern.compile("/\\*.*?\\*/|^\\s*//[^\\n]*", Pattern.DOTALL | Pattern.MULTILINE);

    @Test
    void everyWriteThroughARuleSystemGoesThroughTheGuard() {
        Map<String, String> escriben = new TreeMap<>();
        Set<String> sinGuarda = new TreeSet<>();
        for (Path fichero : ficheros()) {
            String relativo = FUENTES.relativize(fichero).toString().replace('\\', '/');
            if (relativo.equals(PUERTO) || relativo.contains("/infrastructure/persistence/")) {
                continue;
            }
            String codigo = COMENTARIO.matcher(leer(fichero)).replaceAll("");
            String escritura = primeraEscritura(codigo);
            if (escritura == null) {
                continue;
            }
            escriben.put(relativo, escritura);
            if (!LLAMA_A_LA_GUARDA.matcher(codigo).find()) {
                sinGuarda.add(relativo);
            }
        }

        TreeSet<String> sinPermiso = new TreeSet<>(sinGuarda);
        sinPermiso.removeAll(ESCRIBEN_POR_CAPA.keySet());

        TreeSet<String> excepcionesSobrantes = new TreeSet<>(ESCRIBEN_POR_CAPA.keySet());
        excepcionesSobrantes.removeAll(sinGuarda);

        StringBuilder mensaje = new StringBuilder();
        sinPermiso.forEach(fichero -> mensaje.append("\n  ").append(fichero)
                .append(" escribe rule_entity sin comprobar su capa: «").append(escriben.get(fichero)).append("»"));
        if (!sinPermiso.isEmpty()) {
            mensaje.append("\nLlama a NationalLayerGuard antes de escribir: desde una reglamentación sólo se escribe"
                    + " en su capa nacional. Si escribe por capa, apúntalo en ESCRIBEN_POR_CAPA con su motivo.");
        }
        excepcionesSobrantes.forEach(fichero -> mensaje.append("\n  ").append(fichero)
                .append(" está en ESCRIBEN_POR_CAPA y ya no escribe sin guarda (o no existe): quítalo de la lista."));

        assertTrue(mensaje.isEmpty(), mensaje.toString());
        assertTrue(escriben.keySet().containsAll(Set.of(
                        "com/b4rrhh/rulesystem/application/usecase/CorrectRuleEntityService.java",
                        "com/b4rrhh/rulesystem/application/usecase/DeleteRuleEntityService.java")),
                "El test ya no encuentra a los que sabe que escriben: se ha roto la forma de buscarlos, no el código. "
                        + "Encontrados: " + escriben.keySet());
    }

    /** La primera llamada de escritura sobre un campo de tipo {@code RuleEntityRepository}, o {@code null}. */
    private static String primeraEscritura(String codigo) {
        Set<String> campos = CAMPO_DEL_PUERTO.matcher(codigo).results()
                .map(resultado -> resultado.group(1))
                .collect(Collectors.toSet());
        for (String campo : campos) {
            Matcher escritura = Pattern.compile("\\b" + Pattern.quote(campo) + "\\s*\\.\\s*(?:" + ESCRITURAS + ")\\s*\\(")
                    .matcher(codigo);
            if (escritura.find()) {
                return escritura.group();
            }
        }
        return null;
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
