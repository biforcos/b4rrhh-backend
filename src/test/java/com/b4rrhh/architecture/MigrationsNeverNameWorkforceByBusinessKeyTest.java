package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una migración cambia el esquema y los catálogos. No nombra a nadie de la plantilla
 * (backend#74).
 *
 * <p>La V118 partía la jornada de <em>EMP001000</em> buscándolo por su número de empleado. A ese
 * empleado lo crea el loader hablando por la API, mucho después de que Flyway haya corrido, así
 * que el {@code update} se ejecutaba siempre sobre una tabla donde ese empleado todavía no
 * existía. <strong>Cero filas afectadas no es un error</strong>: la migración salía en verde, el
 * historial de Flyway la daba por aplicada y el corte de jornada no existía en ninguna parte. No
 * es que fallara a veces — no podía funcionar nunca, y nada lo decía.
 *
 * <p>El defecto no era la V118, que ya no se puede tocar: era que nada impide escribir la V127
 * con el mismo error mañana. Esto es lo que lo impide.
 *
 * <h3>Qué se busca</h3>
 *
 * <p>Una de las {@link #CLAVES_DE_NEGOCIO} comparada con un literal. Ésas son las columnas con
 * las que se señala a un individuo concreto —un empleado, una presencia suya, el centro o el
 * coste al que está adscrito—, y una migración que escribe una de ellas en su {@code where} está
 * apuntando a una fila que otro proceso tiene que haber creado antes. Los catálogos no aparecen
 * aquí: sembrar un centro de trabajo o una empresa es {@code insert into ... values}, no
 * comparar, y por eso las V65–V94 pasan sin ruido.
 *
 * <h3>Dos límites conocidos, y por qué se aceptan</h3>
 *
 * <p><strong>No se distingue leer de escribir.</strong> El criterio del issue hablaba de
 * {@code update} y {@code delete}, pero se mira cualquier sentencia, por dos razones: una
 * migración no tiene nada que hacer nombrando a un individuo tampoco para leerlo, y un bloque
 * {@code do $$ ... $$} —que las V109 y V121–V123 usan— esconde sus escrituras de cualquier
 * analizador barato. Preferimos el falso positivo, que se resuelve apuntándolo en
 * {@link #EXCEPCIONES} con su motivo delante.
 *
 * <p><strong>No se detecta una clave de negocio que llegue por parámetro o por concatenación.</strong>
 * Sólo se ve el literal. Es el mismo tipo de limitación que declara
 * {@link EveryCatalogVerticalDeclaresItsRuleEntityUsageTest}: la guardia cubre la forma en que el
 * fallo se escribió de verdad, no todas las formas imaginables.
 */
class MigrationsNeverNameWorkforceByBusinessKeyTest {

    private static final Path MIGRACIONES = Path.of("src/main/resources/db/migration");

    /**
     * Las columnas con las que una sentencia señala a un individuo. No son códigos de catálogo:
     * son la forma de decir «esta persona», «este centro», «este coste».
     */
    private static final Pattern CLAVES_DE_NEGOCIO = Pattern.compile(
            "(employee_number|presence_number|work_center_code|cost_center_code|company_code)"
                    + "\\s*(=|<>|!=|\\bin\\b|\\blike\\b)\\s*\\(?\\s*'([^']*)'",
            Pattern.CASE_INSENSITIVE);

    /**
     * Las excepciones se listan una a una y con su motivo, como el ADR-054 hace con los
     * catálogos. Añadir una entrada aquí es un acto visible en el diff; que lo sea es todo el
     * sentido de esta guardia.
     */
    private static final Map<String, String> EXCEPCIONES = Map.of(
            "V118", """
                    Es la migración que originó el issue, y no se puede arreglar: editarla cambia su
                    checksum, y el flyway_schema_history viaja dentro del volcado de la demo, así que
                    el siguiente reset-demo.sh dejaría la API sin arrancar. Su update de
                    employee.working_time no se ha ejecutado nunca, en ningún camino de construcción;
                    la V126 lo deja escrito en la propia historia de migraciones. Se queda byte a byte
                    como está.""");

    @Test
    void ningunaMigracionNombraAUnIndividuoPorSuClaveDeNegocio() {
        assertTrue(Files.isDirectory(MIGRACIONES), "No encuentro " + MIGRACIONES.toAbsolutePath());

        Map<String, List<String>> delatadas = new LinkedHashMap<>();
        for (Path migracion : migracionesEnOrden()) {
            if (EXCEPCIONES.containsKey(version(migracion))) {
                continue;
            }
            List<String> hallazgos = clavesDeNegocioDe(sinComentarios(leer(migracion)));
            if (!hallazgos.isEmpty()) {
                delatadas.put(migracion.getFileName().toString(), hallazgos);
            }
        }

        assertTrue(delatadas.isEmpty(), """
                Estas migraciones nombran a un individuo por su clave de negocio: %s

                Una migración corre sobre el esquema, antes de que exista nadie; los empleados y su
                historia los crea el loader después, hablando por la API. Una sentencia que busca a
                alguien por su número no encuentra nada y no da error: se aplica en verde y no hace
                nada, que es peor que fallar. Es lo que le pasó a la V118 (backend#74).

                El dato va donde se crea el dato: al generador de escenarios del loader, o a un paso
                de post-siembra del deploy. Si de verdad es una excepción legítima, se añade a
                EXCEPCIONES con su motivo escrito.
                """.formatted(formatear(delatadas)));
    }

    /**
     * La guardia tiene que seguir viendo el fallo que vino a impedir. Sin esto, cualquiera puede
     * ablandar el patrón y dejar la suite en verde sin que nada proteja ya nada.
     */
    @Test
    void laGuardiaSigueViendoElUpdateDeLaV118() {
        Path v118 = migracionesEnOrden().stream()
                .filter(migracion -> version(migracion).equals("V118"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No encuentro la V118 en " + MIGRACIONES));

        List<String> hallazgos = clavesDeNegocioDe(sinComentarios(leer(v118)));

        assertTrue(hallazgos.size() == 2, """
                La V118 busca a EMP001000 por su número de empleado en dos sentencias —el update de
                employee.working_time y el insert de la ventana partida— y la guardia debería ver las
                dos. Ha visto: %s
                """.formatted(hallazgos));
    }

    @Test
    void todaExcepcionApuntaAUnaMigracionQueExiste() {
        List<String> versiones = migracionesEnOrden().stream().map(this::version).toList();
        List<String> huerfanas = EXCEPCIONES.keySet().stream()
                .filter(version -> !versiones.contains(version))
                .sorted()
                .toList();

        assertTrue(huerfanas.isEmpty(), """
                Estas excepciones no corresponden a ninguna migración: %s

                Una excepción que ya no protege nada es una puerta abierta que nadie vigila. Bórrala.
                """.formatted(huerfanas));
    }

    private List<Path> migracionesEnOrden() {
        try (Stream<Path> ficheros = Files.list(MIGRACIONES)) {
            return ficheros
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String version(Path migracion) {
        String nombre = migracion.getFileName().toString();
        int separador = nombre.indexOf("__");
        return separador < 0 ? nombre : nombre.substring(0, separador);
    }

    private String leer(Path migracion) {
        try {
            return Files.readString(migracion, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> clavesDeNegocioDe(String sql) {
        List<String> hallazgos = new ArrayList<>();
        Matcher matcher = CLAVES_DE_NEGOCIO.matcher(sql);
        while (matcher.find()) {
            hallazgos.add("línea " + lineaDe(sql, matcher.start()) + ": "
                    + matcher.group(1) + " " + matcher.group(2) + " '" + matcher.group(3) + "'");
        }
        return hallazgos;
    }

    private int lineaDe(String texto, int posicion) {
        int linea = 1;
        for (int i = 0; i < posicion; i++) {
            if (texto.charAt(i) == '\n') {
                linea++;
            }
        }
        return linea;
    }

    /**
     * Sustituye los comentarios por espacios en vez de borrarlos, para que los números de línea
     * del informe sigan siendo los del fichero. Respeta las cadenas y el entrecomillado por
     * dólares ({@code $$ ... $$}), donde un {@code --} no abre comentario.
     */
    private String sinComentarios(String sql) {
        StringBuilder limpio = new StringBuilder(sql.length());
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);

            if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    limpio.append(' ');
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                while (i < sql.length() && !(sql.charAt(i) == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/')) {
                    limpio.append(sql.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                for (int j = 0; j < 2 && i < sql.length(); j++, i++) {
                    limpio.append(' ');
                }
                continue;
            }
            if (c == '\'') {
                limpio.append(c);
                i++;
                while (i < sql.length()) {
                    limpio.append(sql.charAt(i));
                    if (sql.charAt(i) == '\'') {
                        i++;
                        break;
                    }
                    i++;
                }
                continue;
            }
            int finEtiqueta = finDeEtiquetaDolar(sql, i);
            if (finEtiqueta > 0) {
                String etiqueta = sql.substring(i, finEtiqueta);
                int cierre = sql.indexOf(etiqueta, finEtiqueta);
                int hasta = cierre < 0 ? sql.length() : cierre + etiqueta.length();
                limpio.append(sql, i, hasta);
                i = hasta;
                continue;
            }

            limpio.append(c);
            i++;
        }
        return limpio.toString();
    }

    /** Devuelve el final de una etiqueta {@code $$} o {@code $nombre$} que empiece en la posición dada, o -1. */
    private int finDeEtiquetaDolar(String sql, int desde) {
        if (sql.charAt(desde) != '$') {
            return -1;
        }
        int i = desde + 1;
        while (i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
            i++;
        }
        return i < sql.length() && sql.charAt(i) == '$' ? i + 1 : -1;
    }

    private String formatear(Map<String, List<String>> delatadas) {
        StringBuilder informe = new StringBuilder();
        delatadas.forEach((fichero, hallazgos) -> {
            informe.append("\n  ").append(fichero);
            hallazgos.forEach(hallazgo -> informe.append("\n      ").append(hallazgo));
        });
        return informe.toString();
    }
}
