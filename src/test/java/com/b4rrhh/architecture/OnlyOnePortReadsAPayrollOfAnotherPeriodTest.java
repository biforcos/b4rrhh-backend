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
 * Un calculo lee un recibo de otro periodo por <b>un solo camino</b>, y ese camino filtra por
 * {@code DEFINITIVE} ({@code backend#128}, ADR-074).
 *
 * <h2>Que vigila, y por que no lo puede vigilar un test de comportamiento</h2>
 *
 * <p>Que la base reguladora se niegue a leer un recibo que todavia puede cambiar lo prueba el
 * {@code TheRegulatoryBaseOnlyReadsAClosedPreviousMonthTest}, y se ve rojo si se relaja. Lo que ese
 * test no puede ver es <b>una segunda lectura</b>: el dia que los atrasos del paso 6 necesiten saber
 * cuanto valia agosto, la forma natural de escribirlo es una consulta nueva, y una consulta nueva no
 * pone en rojo el test de la base reguladora.
 *
 * <p>El ADR-069 puso el mismo candado hacia fuera —ninguna salida lee nada que no sea definitivo— y
 * este es el de dentro: <b>ningun calculo lee un recibo de otro periodo que no sea definitivo</b>. La
 * diferencia entre los dos es a quien se le miente. Una remesa con un numero provisional se
 * reconcilia mal; una prestacion calculada sobre una base que manana es otra ya esta entregada.
 *
 * <h2>Como lo mira</h2>
 *
 * <p>Busca en {@code src/main/java} todo lo que consulte la tabla de recibos —JPQL sobre
 * {@code PayrollEntity} o SQL sobre {@code payroll.payroll}— y exige que cada fichero este en la
 * lista de abajo, con su motivo escrito. Un camino nuevo sale rojo en el commit que lo anade.
 *
 * <p>Es el patron del {@code NoOutputReadsAnythingButDefinitiveTest} y del
 * {@code TerminationCoversEveryPresenceVerticalTest}: se lee el fuente, sin ArchUnit, para que la
 * regla se pueda comprobar de un vistazo.
 *
 * <p>Limitacion, dicha: se miran nombres. Una lectura que llegue a los recibos por un camino que no
 * se llame asi no se vera, y entonces este test hay que reescribirlo, no borrarlo.
 */
class OnlyOnePortReadsAPayrollOfAnotherPeriodTest {

    private static final Path FUENTES = Path.of("src/main/java");

    /**
     * Quien puede consultar la tabla de recibos, y para que.
     *
     * <p>La clave es el nombre del fichero; el valor, el motivo. Que el motivo este escrito es la
     * mitad que hace util a la lista: una linea anadida sin motivo es exactamente lo que este test
     * existe para convertir en una conversacion.
     */
    private static final Map<String, String> PUEDEN_LEER_RECIBOS = new TreeMap<>(Map.of(
            "SpringDataPayrollRepository.java",
                    "el repositorio del vertical. Lee el recibo de SU unidad por clave de negocio, la"
                            + " lista de una pantalla, y -para la base reguladora- el estado y la base"
                            + " de OTRO periodo, esto ultimo filtrando por DEFINITIVE en la consulta",
            "DemoCountsQuery.java",
                    "cuenta filas para la pantalla de la demo: un count(*) sobre la tabla entera, sin"
                            + " periodo y sin leer ningun importe"));

    /** El unico puerto por el que un calculo puede leer un recibo de otro periodo. */
    private static final String PUERTO = "PreviousPeriodContributionBaseLookupPort";

    /** El filtro, nombrado donde se lee y no tres llamadas mas abajo (ADR-069 §2). */
    private static final String FILTRO = "PayrollStatus.DEFINITIVE";

    /**
     * Las tres formas de preguntar por recibos que hay en este codigo: JPQL sobre la entidad, SQL con
     * la tabla escrita, y el nombre de la tabla como literal para pegarlo a un {@code select}.
     *
     * <p>La tercera no sobra: {@code DemoCountsQuery} arma su {@code count(*)} concatenando, y sin
     * ella una lectura escrita asi no se veria.
     *
     * <p>El {@code \b} del final es lo que impide que {@code payroll.payroll_object_binding} y
     * {@code payroll.payroll_table_row} —que son catalogo y no recibos— salgan como falsos positivos.
     */
    private static final Pattern CONSULTA_RECIBOS = Pattern.compile(
            "from\\s+PayrollEntity\\b|from\\s+payroll\\.payroll\\b|\"payroll\\.payroll\"",
            Pattern.CASE_INSENSITIVE);

    @Test
    void nadieMasQueElPuertoConsultaLaTablaDeRecibos() {
        var encontrados = new TreeSet<String>();
        for (Path fuente : ficherosJava(FUENTES)) {
            if (CONSULTA_RECIBOS.matcher(leer(fuente)).find()) {
                encontrados.add(fuente.getFileName().toString());
            }
        }

        assertEquals(new TreeSet<>(PUEDEN_LEER_RECIBOS.keySet()), encontrados, """
                La lista de quien consulta la tabla de recibos no es la que se ha decidido.

                  declarados: %s
                  en el codigo: %s

                Si sobra uno, es una lectura nueva: si lee recibos de OTRO periodo, tiene que pasar
                por %s, que filtra por %s en un solo sitio (ADR-074). Si lee los de su propio
                periodo, entra en la lista de arriba con su motivo escrito.

                Una prestacion —o un atraso— calculado sobre un recibo que todavia puede cambiar no
                se distingue de uno bueno hasta que el recibo ya esta entregado.

                Si falta uno, la busqueda ha dejado de encontrar las consultas y este test es verde
                sin haber mirado nada: hay que reescribirlo, no borrarlo.
                """.formatted(PUEDEN_LEER_RECIBOS.keySet(), encontrados, PUERTO, FILTRO));
    }

    @Test
    void laLecturaDeOtroPeriodoFiltraPorDefinitivoDondeSeLee() {
        Path repositorio = FUENTES.resolve(
                "com/b4rrhh/payroll/infrastructure/persistence/SpringDataPayrollRepository.java");
        assertTrue(Files.isRegularFile(repositorio),
                "No encuentro " + repositorio + ". Si el repositorio se ha movido, este test hay que"
                        + " reescribirlo, no borrarlo.");

        assertTrue(leer(repositorio).contains(FILTRO), """
                La consulta que lee la base de otro periodo no nombra %s.

                El filtro va escrito en la consulta y no pasado como parametro: un parametro se puede
                pasar mal desde otro sitio, y esta lectura no admite otro estado (ADR-069 §2).
                """.formatted(FILTRO));
    }

    @Test
    void hayExactamenteUnaImplementacionDelPuerto() {
        List<String> implementaciones = ficherosJava(FUENTES).stream()
                .filter(fuente -> leer(fuente).contains("implements " + PUERTO))
                .map(fuente -> fuente.getFileName().toString())
                .sorted()
                .toList();

        assertEquals(List.of("PreviousPeriodContributionBaseLookupAdapter.java"), implementaciones,
                "El puerto que lee otro periodo tiene que tener UNA implementacion: si hay dos, el"
                        + " filtro esta escrito dos veces y la segunda es la que se queda atras."
                        + " Encontradas: " + implementaciones);
    }

    private static List<Path> ficherosJava(Path raiz) {
        try (Stream<Path> arbol = Files.walk(raiz)) {
            return arbol.filter(path -> path.toString().endsWith(".java")).sorted().toList();
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
