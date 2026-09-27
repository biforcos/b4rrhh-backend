package com.b4rrhh.architecture;

import com.b4rrhh.payroll.retro.application.service.RetroDeltaCalculator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que no viaja al atraso es <b>exactamente</b> lo que la invariante no puede comparar
 * ({@code backend#133}).
 *
 * <h2>Que vigila, y por que no lo puede vigilar un test de comportamiento</h2>
 *
 * <p>Hay dos listas de conceptos en dos sitios:
 *
 * <ul>
 *   <li>{@code RetroDeltaCalculator.NO_VIAJAN} — los que <b>no se atribuyen</b> a su mes de origen, asi
 *       que no generan linea de atraso.</li>
 *   <li>La consulta de la invariante ({@code docs/consultas/invariante-del-atraso.sql} y el escenario)
 *       — los que la invariante <b>excluye</b>.</li>
 * </ul>
 *
 * <p><b>Tienen que ser la misma</b>, y no por simetria: son la misma cosa dicha dos veces. Un concepto
 * que no viaja tiene, por construccion, el vigente de su mes distinto de lo pagado por su mes —porque
 * nadie pago su diferencia—, asi que si la invariante lo mirara saldria roja siempre. Y al contrario: un
 * concepto que la invariante excluya pero que si viaje es un concepto cuya diferencia se paga y que nadie
 * comprueba.
 *
 * <p>El escenario del {@code #133} se pondria rojo con el primer caso —la invariante empezaria a fallar—
 * pero <b>no con el segundo</b>: excluir de la invariante un concepto que viaja no rompe nada, deja de
 * vigilar algo. Eso es lo que este candado cubre.
 *
 * <p>Y es como empezo: el issue decia «excluido el 800», y al montar la invariante salieron siete mas.
 * Esa lista va a volver a crecer el dia que aparezca otro concepto del mes que paga, y cuando crezca
 * tiene que crecer en los dos sitios.
 *
 * <h2>Como lo mira</h2>
 *
 * <p>Lee las dos listas del fuente y las compara. Es el patron del
 * {@code TheUnionOfCutsCoversEveryVerticalThatBreaksThePeriodTest}: se lee el fichero, sin ArchUnit, para
 * que la regla se pueda comprobar de un vistazo.
 */
class TheInvariantAndTheDeltaAgreeOnWhatDoesNotTravelTest {

    private static final Path ESCENARIO = Path.of(
            "src/test/java/com/b4rrhh/payroll/scenario/"
                    + "TheArrearIsTheDifferenceAgainstWhatWasPaidAndNotAgainstTheReceiptTest.java");

    private static final Path CONSULTA = Path.of("docs/consultas/invariante-del-atraso.sql");

    /** {@code not in ('800', '970', ...)} */
    private static final Pattern LISTA_DE_LA_CONSULTA = Pattern.compile(
            "not in\\s*\\n?\\s*\\(([^)]*)\\)", Pattern.DOTALL);

    private static final Pattern CODIGO = Pattern.compile("['\"]([A-Z0-9_]{2,})['\"]");

    @Test
    void theDeltaAndTheInvariantExcludeExactlyTheSameConcepts() {
        // El conjunto de verdad y no una expresión regular sobre el fuente: desde el backend#140 se
        // escribe como la unión de lo que es —la retención, su base y los totales—, y leerlo del fichero
        // obligaría a este test a saber cómo se escribe una unión.
        TreeSet<String> noViajan = new TreeSet<>(RetroDeltaCalculator.NO_VIAJAN);
        TreeSet<String> excluidosEnElEscenario = codigosDe(LISTA_DE_LA_CONSULTA, leer(ESCENARIO), ESCENARIO);
        TreeSet<String> excluidosEnLaConsulta = codigosDe(LISTA_DE_LA_CONSULTA, leer(CONSULTA), CONSULTA);

        // La retención y SU BASE (backend#140). El IRPF no viaja porque se retiene sobre lo que se paga
        // cuando se paga (ADR-070 §4), y su base es la mitad de esa decisión que nadie aplicó: el B09 salía
        // como línea de atraso por el total de atrasos que el mes había pagado.
        assertTrue(noViajan.containsAll(java.util.List.of("800", "B09")),
                "Ni la retención ni su base viajan: " + noViajan);

        assertEquals(noViajan, excluidosEnElEscenario, """
                La lista de lo que no viaja y la de lo que la invariante excluye no son la misma.

                  no viajan (RetroDeltaCalculator):        %s
                  excluidos (la invariante del escenario): %s

                Tienen que ser la misma, y no por simetria: son la misma cosa dicha dos veces. Un
                concepto que no viaja tiene el vigente de su mes distinto de lo pagado por su mes
                -porque nadie pago su diferencia-, asi que si la invariante lo mirara saldria roja
                siempre. Y un concepto que la invariante excluya pero que si viaje es una diferencia
                que se paga y que nadie comprueba.
                """.formatted(noViajan, excluidosEnElEscenario));

        assertEquals(noViajan, excluidosEnLaConsulta, """
                La consulta pegable de docs/consultas/invariante-del-atraso.sql excluye otra lista que
                el codigo.

                  no viajan:              %s
                  excluidos en el .sql:   %s

                Esa consulta es la que alguien va a pegar contra la semilla para firmar un criterio, asi
                que si dice otra cosa que el codigo, el criterio se firma contra una comprobacion que no
                es la que corre en los tests.
                """.formatted(noViajan, excluidosEnLaConsulta));
    }

    private static TreeSet<String> codigosDe(Pattern lista, String contenido, Path donde) {
        Matcher m = lista.matcher(contenido);
        assertTrue(m.find(), "No encuentro la lista de conceptos en " + donde.toAbsolutePath()
                + ". Si se ha movido o se ha escrito de otra forma, este test hay que reescribirlo, no"
                + " borrarlo: la regla que protege sigue siendo cierta.");
        TreeSet<String> codigos = new TreeSet<>();
        Matcher c = CODIGO.matcher(m.group(1));
        while (c.find()) {
            codigos.add(c.group(1));
        }
        return codigos;
    }

    private static String leer(Path fichero) {
        assertTrue(Files.isRegularFile(fichero),
                "No encuentro " + fichero.toAbsolutePath());
        try {
            return Files.readString(fichero, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
