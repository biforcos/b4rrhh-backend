package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ninguna salida lee nada que no sea {@code DEFINITIVE} (ADR-069).
 *
 * <h2>Que vigila</h2>
 *
 * <p>Una <b>salida</b> es algo que pone un dato de nomina en manos de alguien de fuera: un
 * documento entregado, un fichero, una remesa, una declaracion, un asiento. Todas leen la foto fija
 * del mes —los recibos cerrados— o no leen. Ensenar no es entregar: la pantalla puede pintar un
 * {@code CALCULATED} marcado como borrador, y eso no es una salida.
 *
 * <h2>Por que esto se escanea y no se prueba</h2>
 *
 * <p>Porque hoy hay <b>una</b> salida y cumple, y lo que hay que defender es la segunda. Un test de
 * comportamiento no ve un camino que todavia no existe; el escaneo si, y falla en el commit que lo
 * anade en vez de en la conciliacion del mes siguiente. Es el patron del
 * {@code EveryPathToDefinitiveArchivesItsDocumentTest} y del
 * {@code TerminationCoversEveryPresenceVerticalTest}: se lee el codigo fuente, sin ArchUnit, para
 * que la regla se pueda comprobar de un vistazo.
 *
 * <h2>Como encuentra las salidas</h2>
 *
 * <p>Por donde viven. El ADR-069 §3 decide que cada salida es un vertical bajo
 * {@code com.b4rrhh.payroll}, y un vertical se reconoce por tener capa de aplicacion. Asi una
 * salida nueva entra en la vigilancia el dia que se crea su carpeta y no el dia que alguien se
 * acuerda de registrarla aqui.
 *
 * <p>Si algun dia aparece bajo {@code payroll/} un vertical que <b>no</b> sea una salida, este test
 * le exigira filtrar y saldra rojo. Eso es lo que se quiere: que la respuesta sea una decision
 * escrita —anadirlo abajo, con su motivo— y no un descuido.
 *
 * <h2>Limitaciones, dichas</h2>
 *
 * <p>Se miran nombres. Una salida que se traiga los recibos por un camino que no se llame como el
 * de hoy no se vera, igual que en los otros tests de arquitectura de este repositorio. Y el corte
 * en metodos es por sangria: en este codigo todos los miembros de una clase van a cuatro espacios,
 * asi que basta, pero no es un parser de Java y no pretende serlo.
 */
class NoOutputReadsAnythingButDefinitiveTest {

    private static final Path CONTEXTO = Path.of("src/main/java/com/b4rrhh/payroll");

    /**
     * Verticales bajo {@code payroll/} que no son salidas, con su motivo.
     *
     * <p>{@code basesalary} busca filas de tabla salarial para el motor: no saca nada del sistema y
     * no lee recibos. De hecho no tiene capa de aplicacion, asi que hoy no haria falta nombrarlo;
     * esta escrito para que el dia que la tenga, la pregunta se haga.
     */
    private static final List<String> NO_SON_SALIDAS = List.of("basesalary");

    /** Carpetas de capa del vertical raiz, que no son verticales. */
    private static final List<String> CAPAS = List.of("application", "domain", "infrastructure");

    /**
     * Traerse recibos de donde vivan: una llamada sobre algo que nombra la nomina y es una fuente
     * —repositorio, puerto de lectura, buscador—.
     *
     * <p>No casa con {@code payroll.getStatus()} ni con {@code this.payrollRepository = ...}: hace
     * falta el sufijo de fuente y la llamada.
     */
    private static final Pattern LEE_RECIBOS =
            Pattern.compile("\\b\\w*[Pp]ayroll\\w*(?:Repository|ReadPort|Port|Finder)\\.\\w+\\(");

    /** El filtro, nombrado donde se lee y no tres llamadas mas abajo (ADR-069 §2). */
    private static final String FILTRO = "PayrollStatus.DEFINITIVE";

    /** Inicio de miembro de clase en este codigo: cuatro espacios y un modificador. */
    private static final Pattern MIEMBRO =
            Pattern.compile("^ {4}(?:public|private|protected|static)\\b", Pattern.MULTILINE);

    @Test
    void everyOutputFiltersByDefinitiveWhereItReadsPayrolls() {
        List<String> salidas = salidas();

        assertEquals(List.of("document"), salidas,
                "Las salidas son los verticales bajo payroll/ (ADR-069 §3). Si sale una de mas, es "
                        + "nueva y tiene que filtrar por DEFINITIVE; si no es una salida, va a "
                        + "NO_SON_SALIDAS con su motivo escrito. Si sale una de menos, la busqueda "
                        + "ha dejado de encontrarlas y este test ya no vigila nada.");

        List<String> sinFiltrar = new ArrayList<>();
        List<String> filtrando = new ArrayList<>();

        for (String salida : salidas) {
            for (Path fuente : ficherosJava(CONTEXTO.resolve(salida))) {
                String contenido = leer(fuente);
                int metodo = 0;
                for (String trozo : MIEMBRO.split(contenido)) {
                    metodo++;
                    if (!LEE_RECIBOS.matcher(trozo).find()) {
                        continue;
                    }
                    String donde = fuente + " (miembro " + metodo + ")";
                    if (trozo.contains(FILTRO)) {
                        filtrando.add(donde);
                    } else {
                        sinFiltrar.add(donde);
                    }
                }
            }
        }

        assertTrue(sinFiltrar.isEmpty(), """
                Estos metodos de una salida se traen recibos y no nombran %s:

                  %s

                Una salida lee la foto fija del mes o no lee (ADR-069). Un recibo CALCULATED en una
                remesa es una transferencia hecha con un numero provisional, y no se distingue de
                una buena hasta que alguien la concilie. El filtro va donde se lee, no tres llamadas
                mas abajo.

                Si esto es una pantalla y no una salida, no va bajo payroll/<vertical>/: ensenar un
                borrador marcado es legitimo y el ADR-069 §1 lo dice.
                """.formatted(FILTRO, String.join("\n  ", sinFiltrar)));

        // Y que la lista no este vacia por haberse quedado sin buscar.
        assertEquals(1, filtrando.size(),
                "Hoy hay un solo sitio donde una salida se trae recibos —el documento del recibo, "
                        + "que se sirve del almacen si esta cerrado— y tiene que salir aqui. Si "
                        + "sale ninguno, la busqueda ha dejado de encontrar la lectura y el test "
                        + "es verde sin haber mirado nada; si salen mas, hay lecturas nuevas y "
                        + "conviene mirarlas: " + filtrando);
    }

    /** Un vertical bajo payroll/ es una carpeta con capa de aplicacion. */
    private static List<String> salidas() {
        try (Stream<Path> hijos = Files.list(CONTEXTO)) {
            return hijos
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(nombre -> !CAPAS.contains(nombre))
                    .filter(nombre -> !NO_SON_SALIDAS.contains(nombre))
                    .filter(nombre -> Files.isDirectory(CONTEXTO.resolve(nombre).resolve("application")))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
