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
 * Un recibo definitivo tiene su documento, se haya cerrado por donde se haya cerrado
 * ({@code backend#112}).
 *
 * <h2>Por que esto se escanea y no se prueba</h2>
 *
 * <p>Porque lo que hay que defender no es que los dos caminos de hoy archiven —eso lo prueban sus
 * propios tests— sino que <b>el tercero, cuando lo haya, no pueda no archivar</b>. Un test de
 * comportamiento no ve un camino que todavia no existe; el escaneo si, y falla en el commit que lo
 * anade en vez de en la demo tres meses despues.
 *
 * <p>Es el patron del {@code TerminationCoversEveryPresenceVerticalTest}: alli se vigila que
 * ninguna vertical cerrable se caiga del cese, aqui que ninguna transicion a {@code DEFINITIVE} se
 * quede sin papel. La pregunta es una sola y por eso se puede hacer: quien llama a
 * {@code finalizePayroll()} sobre el agregado, ¿llama tambien al archivador?
 *
 * <p>El issue lo dice sin rodeos: un {@code DEFINITIVE} sin su documento es una promesa rota. Este
 * fichero es lo que impide romperla por descuido.
 */
class EveryPathToDefinitiveArchivesItsDocumentTest {

    private static final Path FUENTES = Path.of("src/main/java");

    /**
     * La llamada sin argumentos sobre una variable, que es la transicion del agregado.
     *
     * <p>No casa con {@code finalizePayroll(command)} —el metodo del caso de uso, que el controlador
     * llama y que no cierra nada por si mismo— ni con la declaracion del propio agregado.
     */
    private static final Pattern TRANSICION = Pattern.compile("\\b\\w+\\.finalizePayroll\\(\\)");

    private static final String ARCHIVADOR = "PayslipDocumentArchiver";

    @Test
    void everyCallerOfTheTransitionAlsoArchivesTheDocument() {
        List<String> sinArchivar = new ArrayList<>();
        List<String> archivando = new ArrayList<>();

        for (Path fuente : ficherosJava()) {
            String contenido = leer(fuente);
            if (!TRANSICION.matcher(contenido).find()) {
                continue;
            }
            if (contenido.contains(ARCHIVADOR)) {
                archivando.add(fuente.getFileName().toString());
            } else {
                sinArchivar.add(fuente.toString());
            }
        }

        assertTrue(sinArchivar.isEmpty(),
                "Estos ficheros llevan un recibo a DEFINITIVE y no emiten su documento. Un "
                        + "definitivo sin papel es una promesa rota, y el papel se emite al "
                        + "cerrar, no despues:\n  " + String.join("\n  ", sinArchivar));

        // Y que la lista no este vacia por haberse quedado sin buscar: si alguien renombra la
        // transicion, el bucle de arriba pasaria por cero ficheros y este test seria verde sin
        // haber mirado nada.
        assertEquals(2, archivando.size(),
                "Hoy hay dos caminos a DEFINITIVE —cerrar uno y cerrar en masa— y los dos tienen "
                        + "que salir aqui. Si salen menos, la busqueda ha dejado de encontrar la "
                        + "transicion; si salen mas, hay un camino nuevo y conviene mirarlo: "
                        + archivando);
    }

    private static List<Path> ficherosJava() {
        try (Stream<Path> arbol = Files.walk(FUENTES)) {
            return arbol.filter(path -> path.toString().endsWith(".java")).toList();
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
