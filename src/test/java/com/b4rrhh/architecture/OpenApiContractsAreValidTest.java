package com.b4rrhh.architecture;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Los contratos de openapi/ son la fuente de verdad del API y de ellos genera el
 * frontend su cliente. Nada en este repo los leia: ni el build, ni la suite,
 * ni la revision. Asi es como un dos puntos suelto en una descripcion sin
 * comillas dejo el contrato de personnel-administration sin parsear durante
 * dos issues, hasta que el frontend intento regenerar su cliente y se encontro
 * el fichero roto en el peor momento, que es cuando otro repo lo necesita
 * (issue #67).
 *
 * Este test es lo que convierte ese descuido en un fallo de la suite, que es
 * lo que el pipeline pasa antes de construir la imagen. Se comprueban dos
 * cosas, en este orden y por separado para que el mensaje diga cual fallo:
 *
 * 1. Que cada contrato es YAML bien formado. Es lo minimo innegociable: un
 *    fichero que no parsea no lo puede consumir nadie.
 * 2. Que ademas es un documento OpenAPI 3 correcto: referencias resueltas,
 *    esquemas con forma, nada que el generador del cliente vaya a rechazar.
 *
 * Se recorren todos los .yaml de openapi/ y no una lista fija a proposito: un
 * contrato nuevo queda protegido sin tocar este test.
 */
class OpenApiContractsAreValidTest {

    private static final Path CONTRATOS = Path.of("openapi");

    @Test
    void theContractsDirectoryExistsAndIsNotEmpty() {
        assertTrue(
                Files.isDirectory(CONTRATOS),
                "No encuentro " + CONTRATOS.toAbsolutePath()
                        + ". Si los contratos se han movido, este test hay que actualizarlo, "
                        + "no borrarlo: la regla que protege sigue siendo cierta."
        );
        assertFalse(contratos().isEmpty(), "No hay ningun .yaml en " + CONTRATOS.toAbsolutePath());
    }

    @TestFactory
    Stream<DynamicTest> everyContractIsWellFormedYaml() {
        return contratos().stream().map(contrato -> DynamicTest.dynamicTest(
                contrato.getFileName() + " es YAML bien formado",
                () -> {
                    try (Reader reader = Files.newBufferedReader(contrato, StandardCharsets.UTF_8)) {
                        new Yaml().compose(reader);
                    } catch (RuntimeException e) {
                        fail(contrato + " no parsea como YAML. Nadie puede generar un cliente con el "
                                + "hasta que se arregle:\n" + e.getMessage(), e);
                    }
                }
        ));
    }

    @TestFactory
    Stream<DynamicTest> everyContractIsAValidOpenApiDocument() {
        return contratos().stream().map(contrato -> DynamicTest.dynamicTest(
                contrato.getFileName() + " es un documento OpenAPI valido",
                () -> {
                    ParseOptions opciones = new ParseOptions();
                    opciones.setResolve(true);
                    SwaggerParseResult resultado = new OpenAPIV3Parser()
                            .readLocation(contrato.toAbsolutePath().toString(), null, opciones);

                    List<String> mensajes = resultado.getMessages() == null ? List.of() : resultado.getMessages();
                    assertNotNull(resultado.getOpenAPI(),
                            contrato + " no se puede leer como OpenAPI:\n" + String.join("\n", mensajes));
                    assertTrue(mensajes.isEmpty(),
                            contrato + " parsea, pero no es un documento OpenAPI correcto:\n"
                                    + String.join("\n", mensajes));
                }
        ));
    }

    private static List<Path> contratos() {
        try (Stream<Path> ficheros = Files.list(CONTRATOS)) {
            return ficheros
                    .filter(f -> f.getFileName().toString().endsWith(".yaml"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
