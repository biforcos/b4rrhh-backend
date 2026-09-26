package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toda escritura con fecha avisa, y avisa por <b>un solo puerto</b> ({@code backend#130}, paso 6 de
 * {@code b4rrhh/workspace#9}).
 *
 * <h2>Que vigila, y por que no lo puede vigilar un test de comportamiento</h2>
 *
 * <p>Que unas horas metidas a un mes cerrado dejen marca lo prueba su escenario, y se ve rojo si se
 * rompe. Lo que ese test no puede ver es <b>una vertical que no avisa</b>: una escritura que se salta
 * el puerto no rompe nada hoy —guarda su fila, devuelve su 200, pasa sus tests— y el dia que alguien
 * le meta algo a un mes cerrado, sencillamente no se recalcula.
 *
 * <p>Y eso no se nota. No hay error, no hay aviso y no hay fila de menos en ningun sitio: hay un
 * empleado que cobra de menos y nadie que pueda decir por que. Es el mismo hueco que cubre el
 * {@code TerminationCoversEveryPresenceVerticalTest} para el cese, y el mismo remedio: la lista de
 * quien tiene que avisar se escribe aqui, y anadir una vertical al arbol sin anadirla al flujo sale
 * rojo en el commit que la anade.
 *
 * <h2>Como lo mira</h2>
 *
 * <p>Recorre {@code src/main/java/com/b4rrhh/employee/&lt;vertical&gt;/application/usecase} de las
 * verticales de abajo, se queda con los ficheros que <b>escriben</b> —los que llaman a un
 * {@code save}, un {@code saveAll}, un {@code update}, un {@code delete...} o un
 * {@code close...ForWindow} de su repositorio— y exige que cada uno nombre
 * {@code DatedWriteNoticePort}. Los que no escriben no se les pide nada, y los que escriben y no
 * avisan a proposito estan en la lista de excepciones con su motivo.
 *
 * <p>Es el patron del {@code OnlyOnePortReadsAPayrollOfAnotherPeriodTest} y del
 * {@code NoOutputReadsAnythingButDefinitiveTest}: se lee el {@code .java}, sin ArchUnit, para que la
 * regla se pueda comprobar de un vistazo.
 *
 * <p>Limitacion, dicha: se miran nombres de metodo. Una escritura que llegue a la tabla por un camino
 * que no se llame asi no se vera, y entonces este test hay que reescribirlo, no borrarlo.
 */
class EveryDatedWriteAnnouncesItselfThroughOnePortTest {

    private static final Path VERTICALES = Path.of("src/main/java/com/b4rrhh/employee");

    /**
     * Las verticales cuyas escrituras pueden cambiar el importe de un recibo cerrado, y por eso
     * tienen que avisar.
     *
     * <p>Las cinco primeras son las que parten el periodo (ADR-068 §2, y la lista esta en el
     * {@code TheUnionOfCutsCoversEveryVerticalThatBreaksThePeriodTest}); la sexta son las entradas de
     * nomina, que no parten nada pero son literalmente el importe; la septima es la distribucion de
     * coste, que no mueve un euro del recibo pero decide a que centro se carga, y lo que se cargo a un
     * mes cerrado tambien se corrige.
     */
    private static final List<String> VERTICALES_QUE_AVISAN = List.of(
            "absence",
            "contract",
            "cost_center",
            "extra_payment_regime",
            "labor_classification",
            "working_time",
            "payroll_input");

    /**
     * Quien escribe una tabla con fecha y <b>no</b> avisa, y por que.
     *
     * <p>Que el motivo este escrito es la mitad que hace util a la lista: una linea anadida sin motivo
     * es exactamente lo que este test existe para convertir en una conversacion.
     */
    private static final Map<String, String> PUEDEN_NO_AVISAR = new TreeMap<>(Map.of());

    /** El puerto, y no hay otro. */
    private static final String PUERTO = "DatedWriteNoticePort";

    /**
     * Como se reconoce una escritura. Son los nombres que este arbol usa: {@code save},
     * {@code saveAll}, {@code update}, {@code delete}, {@code deleteBy...}, {@code deleteAllForWindow},
     * {@code closeAllForWindow} y {@code adjustWindowEndDate}, siempre sobre algo que se llame
     * repositorio.
     */
    private static final Pattern ESCRITURA = Pattern.compile(
            "\\b\\w*[Rr]epository\\s*(?:\\n\\s*)?\\.\\s*(?:save|saveAll|update|delete\\w*|closeAllForWindow"
                    + "|adjustWindowEndDate)\\s*\\(");

    @Test
    void everyUseCaseThatWritesADatedRowAnnouncesItThroughThePort() {
        assertTrue(Files.isDirectory(VERTICALES),
                "No encuentro " + VERTICALES.toAbsolutePath() + ". Si las verticales del empleado se"
                        + " han movido, este test hay que reescribirlo, no borrarlo: la regla que"
                        + " protege sigue siendo cierta.");

        TreeSet<String> escribenYNoAvisan = new TreeSet<>();
        TreeSet<String> vistos = new TreeSet<>();

        for (String vertical : VERTICALES_QUE_AVISAN) {
            Path casos = VERTICALES.resolve(vertical).resolve("application/usecase");
            assertTrue(Files.isDirectory(casos),
                    "No encuentro los casos de uso de la vertical '" + vertical + "' en " + casos
                            + ". Si la vertical se ha renombrado, cambia la lista de arriba en un"
                            + " commit que se vea.");

            for (Path fichero : ficherosDe(casos)) {
                String nombre = fichero.getFileName().toString();
                if (!nombre.endsWith("Service.java")) {
                    continue;
                }
                String fuente = leer(fichero);
                if (!ESCRITURA.matcher(fuente).find()) {
                    // No escribe: un consultor o un planificador. El planificador de cada vertical
                    // -Plan...ChangeService- entra por aqui, y es correcto: es readOnly y solo dice que
                    // pasaria si se escribiera.
                    continue;
                }
                vistos.add(nombre);
                if (PUEDEN_NO_AVISAR.containsKey(nombre)) {
                    continue;
                }
                if (!fuente.contains(PUERTO)) {
                    escribenYNoAvisan.add(vertical + "/" + nombre);
                }
            }
        }

        assertTrue(vistos.size() >= 20,
                "Solo he encontrado " + vistos.size() + " casos de uso que escriban, y hay mas de"
                        + " veinte. Si el patron con el que se reconoce una escritura ha dejado de"
                        + " encajar, este test esta mirando a otro sitio y hay que reescribirlo:"
                        + " encontrados " + vistos);

        assertEquals(new TreeSet<String>(), escribenYNoAvisan, """
                Hay escrituras con fecha que no avisan por el puerto %s.

                  %s

                Una escritura que se salta el puerto no rompe nada hoy: guarda su fila, devuelve su
                200 y pasa sus tests. El dia que alguien le meta algo a un mes que ya tiene recibo
                entregado, ese mes no se recalcula -y no hay error, ni aviso, ni fila de menos en
                ningun sitio: hay un empleado que cobra de menos y nadie que pueda decir por que.

                Lo que hay que hacer es inyectar %s y llamar a notice(...) con la fecha mas antigua a
                la que alcanza el cambio. Si de verdad esta escritura no puede mover el importe de un
                recibo cerrado, ponla en PUEDEN_NO_AVISAR con su motivo escrito, que es lo que
                convierte la excepcion en una decision y no en un olvido.
                """.formatted(PUERTO, String.join("\n  ", escribenYNoAvisan), PUERTO));
    }

    private static List<Path> ficherosDe(Path directorio) {
        try (Stream<Path> s = Files.list(directorio)) {
            return s.filter(Files::isRegularFile).sorted().toList();
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
