package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El periodo lo parte la <b>union</b> de los cambios, y la union son estas cinco (ADR-068,
 * {@code backend#47}, {@code backend#127}).
 *
 * <h2>Que vigila, y por que no lo puede vigilar un test de comportamiento</h2>
 *
 * <p>Que un cambio de jornada parta el mes lo prueba su escenario; que una baja lo parta, el
 * {@code AnAbsenceSplitsThePeriodAndTakesItsDaysAwayTest}. Lo que ninguno de los dos puede ver es que
 * <b>se quite una causa</b>: quitar la de la baja pondria en rojo el escenario de la baja, si es que
 * alguien no lo quita a la vez; quitar la del contrato no pondria en rojo nada, porque hoy ningun
 * concepto lo lee y el ADR-068 §2 dice expresamente que rompe <b>aunque hoy no lo lea nadie</b>.
 *
 * <p>Ese es el caso que este test cubre: una causa que se cae y no se nota. La lista de abajo es la
 * decision de negocio escrita, y el unico modo de cambiarla es cambiarla aqui, en un commit que se
 * vea.
 *
 * <h2>Como lo mira</h2>
 *
 * <p>Lee el fuente y se queda con lo que se le pasa a {@code PayrollPeriodSegmentation.cutsOf(...)}
 * dentro del metodo que arma los tramos. Es el patron del
 * {@code NoOutputReadsAnythingButDefinitiveTest} y del
 * {@code TerminationCoversEveryPresenceVerticalTest}: se lee el {@code .java}, sin ArchUnit, para que
 * la regla se pueda comprobar de un vistazo.
 *
 * <p>Limitacion, dicha: si algun dia los cortes se reunieran por otro camino —un bucle sobre una
 * lista de proveedores, por ejemplo— este test dejaria de verlos y habria que reescribirlo, no
 * borrarlo. La regla que protege seguiria siendo cierta.
 */
class TheUnionOfCutsCoversEveryVerticalThatBreaksThePeriodTest {

    private static final Path SERVICIO = Path.of(
            "src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java");

    /**
     * Las verticales que parten el periodo, y es una decision de negocio (ADR-068 §2).
     *
     * <ul>
     *   <li><b>workingTimeWindows</b> — la jornada: media jornada la mitad del mes son dos precios.
     *   <li><b>agreementWindows</b> — la clasificacion laboral: la categoria decide de que fila de
     *       tabla sale el precio del dia, asi que un ascenso el 16 son dos precios.
     *   <li><b>contractWindows</b> — el contrato: rompe <b>aunque hoy no lo lea ningun concepto</b>,
     *       para que el dia que alguno lo lea ya este partido.
     *   <li><b>extraPaymentRegimeWindows</b> — el regimen de pagas extras: la prorrata de un tramo
     *       prorrateado y la de uno que no lo es entran por puertas distintas (ADR-070).
     *   <li><b>ausenciasQueParten</b> — la ausencia que no se paga: en su tramo no se devengan dias
     *       (ADR-073). No es {@code absenceWindows} a secas porque el lanzador trae todas las
     *       ausencias y el filtro de cuales parten vive en el calculo, en un solo sitio.
     * </ul>
     *
     * <p>El centro de trabajo y la distribucion de coste <b>no estan</b>, y no por no caber: caben
     * con una linea alli y otra en {@code vigenciasEn}. Estan fuera hasta que alguien decida que
     * entran, y esa decision se escribe aqui.
     */
    private static final Set<String> VERTICALES_QUE_PARTEN = new TreeSet<>(List.of(
            "workingTimeWindows",
            "agreementWindows",
            "contractWindows",
            "extraPaymentRegimeWindows",
            "ausenciasQueParten"));

    /** Lo que se le pasa a la particion: {@code cutsOf(input.algo())} o {@code cutsOf(algo(input))}. */
    private static final Pattern CORTES = Pattern.compile(
            "PayrollPeriodSegmentation\\.cutsOf\\(\\s*(?:input\\.)?(\\w+)\\s*\\(");

    @Test
    void everyVerticalThatBreaksThePeriodContributesItsCuts() {
        assertTrue(Files.isRegularFile(SERVICIO),
                "No encuentro " + SERVICIO.toAbsolutePath() + ". Si el calculo de la unidad se ha"
                        + " movido, este test hay que reescribirlo, no borrarlo: la regla que"
                        + " protege sigue siendo cierta.");

        Set<String> enLaUnion = new TreeSet<>();
        Matcher m = CORTES.matcher(leer(SERVICIO));
        while (m.find()) {
            enLaUnion.add(m.group(1));
        }

        assertEquals(VERTICALES_QUE_PARTEN, enLaUnion, """
                La union de los cortes no es la que se ha decidido.

                  declaradas: %s
                  en el codigo: %s

                Si falta una, el periodo ha dejado de partirse por ella y eso no lo nota ningun
                importe: el contrato rompe aunque hoy no lo lea nadie (ADR-068 §2), y una baja que
                deje de partir se paga entera sin que nada avise.

                Si sobra una, es nueva y bienvenida: anadela arriba con su motivo, que es lo que
                convierte la lista en una decision escrita y no en un reflejo del codigo.
                """.formatted(VERTICALES_QUE_PARTEN, enLaUnion));
    }

    private static String leer(Path fichero) {
        try {
            return Files.readString(fichero, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
