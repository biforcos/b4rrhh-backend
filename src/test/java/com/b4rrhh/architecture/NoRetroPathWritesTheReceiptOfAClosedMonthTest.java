package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ningun camino del modo retro escribe el recibo de un mes cerrado ({@code backend#131}, ADR-076).
 *
 * <h2>Que vigila, y por que no lo puede vigilar un test de comportamiento</h2>
 *
 * <p>Que recalcular agosto no le toque el recibo lo prueba su escenario, comparando las filas antes y
 * despues. Lo que ese test no puede ver es <b>un camino nuevo</b>: el dia que alguien necesite escribir
 * algo mas durante una retro —una linea, un aviso, un paso— la forma natural de hacerlo es reusar el
 * servicio que ya sabe escribir recibos, y el test del recibo intacto no se pondria rojo porque el
 * escenario que compara es otro.
 *
 * <p>Y cuando eso pase, lo que se pierde es la unica copia de lo que se le entrego al empleado. No hay
 * error, no hay aviso, y el numero que el empleado tiene impreso deja de existir en el sistema. <b>Asi
 * es como se pudren los motores de nomina</b>, y por eso esto es un candado y no una convencion.
 *
 * <h2>Como lo mira</h2>
 *
 * <p>La vertical {@code payroll/retro} es el modo retro. Este test exige que <b>ninguno</b> de sus
 * ficheros tome los tres caminos que escriben el documento —{@code CalculatePayrollUseCase} (el
 * recibo), {@code PayrollCalculationStepWritePort} (los pasos) y un {@code save} o un {@code delete}
 * sobre el repositorio de recibos (la tabla directamente)—, y que el unico sitio del arbol donde el
 * modo retro y esos caminos se cruzan sea el servicio de la unidad, que los separa con un {@code if} y
 * esta en la lista de abajo con su motivo.
 *
 * <p><b>Leer el recibo si vale.</b> El tercer camino se reconoce por la llamada y no por el nombre del
 * repositorio, y esa diferencia importa: el modo retro <b>tiene</b> que poder preguntar que meses estan
 * cerrados ({@code backend#130}) y cuanto se ha pagado por ellos ({@code backend#133}). Lo que no puede
 * es escribir. Un candado que prohibiera nombrar el repositorio prohibiria la mitad del paso 6.
 *
 * <p>Es el patron del {@code OnlyOnePortReadsAPayrollOfAnotherPeriodTest} y del
 * {@code NoOutputReadsAnythingButDefinitiveTest}: se lee el {@code .java}, sin ArchUnit, para que la
 * regla se pueda comprobar de un vistazo.
 *
 * <p>Limitacion, dicha: se miran nombres. Una escritura que llegue al recibo por un camino que no se
 * llame asi no se vera, y entonces este test hay que reescribirlo, no borrarlo.
 */
class NoRetroPathWritesTheReceiptOfAClosedMonthTest {

    private static final Path FUENTES = Path.of("src/main/java");
    private static final Path MODO_RETRO = Path.of("src/main/java/com/b4rrhh/payroll/retro");

    /**
     * Los tres caminos por los que se <b>escribe</b> el documento de un mes, cada uno con el nombre con
     * el que se cuenta cuando aparece.
     *
     * <p>Los dos primeros son tipos: nombrarlos ya es poder escribir. El tercero es una <b>llamada</b>,
     * porque el repositorio de recibos tambien se lee, y leerlo es legitimo y necesario.
     */
    private static final Map<String, Pattern> ESCRIBEN_EL_RECIBO = new TreeMap<>(Map.of(
            "CalculatePayrollUseCase (escribe el recibo)",
                    Pattern.compile("\\bCalculatePayrollUseCase\\b"),
            "PayrollCalculationStepWritePort (escribe los pasos)",
                    Pattern.compile("\\bPayrollCalculationStepWritePort\\b"),
            "un save o un delete sobre el repositorio de recibos",
                    Pattern.compile("\\b\\w*[Pp]ayroll\\w*Repository\\s*(?:\\n\\s*)?\\.\\s*"
                            + "(?:save|saveAll|delete\\w*)\\s*\\("
                            + "|(?:insert\\s+into|update)\\s+payroll\\.payroll\\b")));

    /**
     * Donde el modo retro y los caminos que escriben el recibo <b>si</b> pueden aparecer juntos, y por
     * que.
     *
     * <p>La clave es el nombre del fichero; el valor, el motivo. Que el motivo este escrito es la mitad
     * que hace util a la lista.
     */
    private static final Map<String, String> PUEDEN_TENER_LOS_DOS = new TreeMap<>(Map.of(
            "CalculatePayrollUnitService.java",
                    "es la costura, y el unico sitio donde las dos mitades se tocan. Calcula igual en los"
                            + " dos modos y bifurca al final: en modo retro escribe el vigente y hace"
                            + " return ANTES de llegar a calculatePayrollUseCase y a writeStepsOf. Que"
                            + " sea el mismo punto es lo que hace comparable un vigente con un recibo"
                            + " (backend#133); que el camino se corte ahi es lo que hace que el recibo"
                            + " entregado no se pueda tocar"));

    /** Como se nombra el modo retro desde fuera de su vertical. */
    private static final Pattern NOMBRA_EL_MODO_RETRO = Pattern.compile(
            "\\b(CurrentCalculation|PayrollCalculationMode\\.CURRENT_CALCULATION|calculateCurrent)\\b");

    @Test
    void noFileOfTheRetroModeCanWriteAReceipt() {
        assertTrue(Files.isDirectory(MODO_RETRO),
                "No encuentro " + MODO_RETRO.toAbsolutePath() + ". Si el modo retro se ha movido, este"
                        + " test hay que reescribirlo, no borrarlo: la regla que protege sigue siendo"
                        + " cierta.");

        List<String> culpables = new ArrayList<>();
        for (Path fuente : ficherosJava(MODO_RETRO)) {
            String contenido = leer(fuente);
            ESCRIBEN_EL_RECIBO.forEach((camino, patron) -> {
                if (patron.matcher(contenido).find()) {
                    culpables.add(fuente.getFileName() + " toma " + camino);
                }
            });
        }

        assertEquals(List.of(), culpables, """
                La vertical del modo retro toma un camino que escribe el recibo:

                  %s

                El calculo vigente es estado y el recibo es un documento (ADR-076). Un motor que deja
                recalcular el recibo de agosto pierde la unica copia de lo que se le entrego al
                empleado, y a partir de ahi ya no se puede explicar una nomina: el numero que el
                empleado tiene impreso deja de existir en el sistema.

                Si de verdad hace falta escribir algo del documento desde aqui, eso no es una retro:
                es un recalculo, y el recalculo tiene su propio camino con su propia regla
                (canBeRecalculated, solo desde NOT_VALID).
                """.formatted(String.join("\n  ", culpables)));
    }

    /**
     * Y el cruce, fuera de la vertical: <b>un solo fichero</b> puede nombrar las dos cosas.
     *
     * <p>El primer test cubre la vertical; este cubre el resto del arbol, que es donde de verdad
     * aparecera el camino nuevo: un servicio que ya escribe recibos y al que alguien le anade el modo
     * retro «para no duplicar».
     */
    @Test
    void onlyTheSeamTakesBothTheRetroModeAndTheReceiptWriters() {
        TreeSet<String> conLosDos = new TreeSet<>();

        for (Path fuente : ficherosJava(FUENTES)) {
            if (fuente.startsWith(MODO_RETRO)) {
                continue;
            }
            String contenido = leer(fuente);
            if (!NOMBRA_EL_MODO_RETRO.matcher(contenido).find()) {
                continue;
            }
            boolean escribeRecibos = ESCRIBEN_EL_RECIBO.values().stream()
                    .anyMatch(patron -> patron.matcher(contenido).find());
            if (escribeRecibos) {
                conLosDos.add(fuente.getFileName().toString());
            }
        }

        assertEquals(new TreeSet<>(PUEDEN_TENER_LOS_DOS.keySet()), conLosDos, """
                Los ficheros que toman a la vez el modo retro y un camino que escribe el recibo no son
                los declarados.

                  declarados: %s
                  en el codigo: %s

                Si sale uno de mas, es un camino nuevo por el que una retro puede llegar al documento
                de un mes entregado. Miralo: o el modo retro no tiene que estar ahi, o esa escritura no
                es una retro. Si de verdad hace falta, anadelo arriba CON SU MOTIVO, que es lo que
                convierte esto en una decision y no en un descuido.

                Si sale uno de menos, la costura se ha movido y este test ya no vigila nada.
                """.formatted(PUEDEN_TENER_LOS_DOS.keySet(), conLosDos));
    }

    private static List<Path> ficherosJava(Path raiz) {
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
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
