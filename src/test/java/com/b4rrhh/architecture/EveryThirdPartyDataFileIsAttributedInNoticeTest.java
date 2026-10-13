package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un fichero de datos de terceros no entra en el repo sin su línea en {@code NOTICE.md}
 * (backend#165).
 *
 * <p>Los códigos postales son de GeoNames, CC BY 4.0, y esa licencia pide atribución. Hasta
 * backend#165 la atribución estaba en la cabecera de la V176, en el {@code LEEME.md} de los datos
 * y en la descripción del endpoint de CP, pero no en {@code NOTICE.md}, que es el único sitio
 * donde la busca quien recibe el código. Nada impedía que el siguiente fichero entrara igual.
 *
 * <p>La guardia es barata a propósito: cada fichero de {@link #DATOS} tiene que aparecer
 * nombrado en {@code NOTICE.md}. No comprueba que la línea diga la licencia correcta —eso lo
 * dice quien la escribe—, sólo que no se pueda olvidar escribirla. El {@code LEEME.md} no es un
 * dato: es el que explica los demás.
 */
class EveryThirdPartyDataFileIsAttributedInNoticeTest {

    private static final Path DATOS = Path.of("src/main/resources/db/geo");

    private static final Path NOTICE = Path.of("NOTICE.md");

    @Test
    void everyDataFileIsNamedInNotice() throws IOException {
        String notice = Files.readString(NOTICE, StandardCharsets.UTF_8);

        List<String> ficheros;
        try (Stream<Path> listado = Files.list(DATOS)) {
            ficheros = listado
                    .filter(Files::isRegularFile)
                    .map(fichero -> fichero.getFileName().toString())
                    .filter(nombre -> !nombre.equals("LEEME.md"))
                    .sorted()
                    .toList();
        }

        assertFalse(ficheros.isEmpty(), "No hay ficheros de datos en " + DATOS + ": la guardia no mira nada");

        List<String> sinAtribuir = ficheros.stream().filter(nombre -> !notice.contains(nombre)).toList();
        assertTrue(sinAtribuir.isEmpty(),
                "Ficheros de datos de terceros sin su línea en NOTICE.md (sección «Datos de terceros»): "
                        + sinAtribuir);
    }
}
