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
 * Nadie compone una URL del almacén (backend#177).
 *
 * <p>La foto guardaba la URL de MinIO tal como la veía el backend que la recibió, y eso no servía a
 * ningún navegador fuera de esa máquina: la demo no podía enseñar una foto. Desde el #177 el almacén no
 * se ve desde fuera; lo que viaja son claves, y los enlaces son de la API. Este escaneo impide que una
 * URL del almacén vuelva a salir por descuido: el {@code endpoint} de MinIO sólo se lee para construir
 * el cliente, y nadie firma URLs.
 */
class NobodyComposesAStorageUrlTest {

    private static final Path FUENTES = Path.of("src/main/java");

    /** Leer el endpoint, firmar una URL o escribir el host del almacén a mano. */
    private static final Pattern URL_DEL_ALMACEN = Pattern.compile(
            "\\.endpoint\\(\\)|getPresignedObjectUrl|GetPresignedObjectUrlArgs|minio:9000|localhost:9000");

    /** El único sitio que puede leer el endpoint: donde se construye el cliente de MinIO. */
    private static final String CONSTRUYE_EL_CLIENTE = "MinioConfig.java";

    @Test
    void onlyTheClientConfigurationReadsTheStorageEndpoint() {
        List<String> componen = new ArrayList<>();
        List<String> loLeen = new ArrayList<>();
        for (Path fuente : ficherosJava()) {
            // Sin comentarios: lo que se vigila es el código, y explicar por qué ya no se hace
            // tiene que poder nombrar lo que se hacía.
            if (!URL_DEL_ALMACEN.matcher(sinComentarios(leer(fuente))).find()) {
                continue;
            }
            if (fuente.getFileName().toString().equals(CONSTRUYE_EL_CLIENTE)) {
                loLeen.add(fuente.getFileName().toString());
            } else {
                componen.add(fuente.toString());
            }
        }

        assertTrue(componen.isEmpty(),
                "Estos ficheros componen o firman una URL del almacén. Desde backend#177 el almacén no "
                        + "se ve desde fuera: se guarda la clave y la API sirve la foto.\n  "
                        + String.join("\n  ", componen));
        // Y que la búsqueda no esté ciega: si el cliente dejara de leer el endpoint así, el bucle de
        // arriba pasaría por cero ficheros y este test sería verde sin haber mirado nada.
        assertEquals(List.of(CONSTRUYE_EL_CLIENTE), loLeen,
                "MinioConfig tiene que seguir leyendo el endpoint para construir el cliente.");
    }

    private static String sinComentarios(String codigo) {
        return codigo.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    private static List<Path> ficherosJava() {
        try (Stream<Path> todos = Files.walk(FUENTES)) {
            return todos.filter(p -> p.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String leer(Path fuente) {
        try {
            return Files.readString(fuente, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
