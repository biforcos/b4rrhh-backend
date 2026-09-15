package com.b4rrhh.payroll.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El fallo de calculo esta en el contrato, no solo en el codigo.
 *
 * <p>Un cliente generado de este contrato solo sabe de las respuestas que el contrato declara. Que
 * el backend conteste 422 con cuerpo y el contrato no lo diga seria volver a lo de siempre: algo
 * servido e invisible para el cliente (backend#80). Se lee el YAML y no el texto, porque
 * «el fichero contiene 422» lo cumple cualquier 422 de cualquier otra ruta.
 */
class RecalculateDeclaresTheCalculationFailureTest {

    private static final Path CONTRATO = Path.of("openapi/personnel-administration-api.yaml");

    private static final String RECALCULATE =
            "/payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}"
                    + "/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/recalculate";

    @Test
    void recalculateDeclaresA422WithTheCommonErrorBody() throws IOException {
        Map<String, Object> contrato = contrato();

        Map<String, Object> respuestas = respuestasDe(contrato, RECALCULATE);

        assertThat(respuestas)
                .withFailMessage("""
                        %s no declara un 422.

                        Un calculo que falla contesta 422 desde el #100. Si no esta aqui, el cliente
                        generado no lo conoce y la pantalla no puede distinguirlo de un fallo del
                        servidor. Declara: %s
                        """.formatted(RECALCULATE, respuestas.keySet()))
                .containsKey("422");

        assertThat(esquemaDe(respuestas.get("422")))
                .withFailMessage("El 422 de %s declara un estado y no un cuerpo. Eso es justo lo que"
                        + " el backend#78 dice que no vale: el cliente sabe que algo fue mal y no"
                        + " puede decir que.", RECALCULATE)
                .isEqualTo("#/components/schemas/PayrollErrorResponse");
    }

    /** Y el cuerpo comun lleva el codigo del suceso, que es lo que la pantalla puede traducir. */
    @Test
    void theCommonErrorBodyCarriesTheRunMessageCode() throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> esquema = (Map<String, Object>) componentes(contrato()).get("PayrollErrorResponse");

        @SuppressWarnings("unchecked")
        Map<String, Object> propiedades = (Map<String, Object>) esquema.get("properties");

        assertThat(propiedades).containsKeys("code", "message", "details");
        assertThat(List.copyOf((List<?>) esquema.get("required")))
                .withFailMessage("code y details son opcionales a proposito: los errores que ya"
                        + " existian (404, 409) no los llevan y su cuerpo no cambia.")
                .isEqualTo(List.of("message"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> respuestasDe(Map<String, Object> contrato, String ruta) {
        Map<String, Object> rutas = (Map<String, Object>) contrato.get("paths");
        Map<String, Object> operaciones = (Map<String, Object>) rutas.get(ruta);
        assertThat(operaciones)
                .withFailMessage("El contrato no tiene la ruta %s. Si se ha movido, este test hay que"
                        + " actualizarlo, no borrarlo.", ruta)
                .isNotNull();
        Map<String, Object> post = (Map<String, Object>) operaciones.get("post");
        return (Map<String, Object>) post.get("responses");
    }

    @SuppressWarnings("unchecked")
    private String esquemaDe(Object respuesta) {
        Map<String, Object> contenido = (Map<String, Object>) ((Map<String, Object>) respuesta).get("content");
        if (contenido == null) {
            return null;
        }
        Map<String, Object> json = (Map<String, Object>) contenido.get("application/json");
        Map<String, Object> esquema = (Map<String, Object>) json.get("schema");
        return (String) esquema.get("$ref");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> componentes(Map<String, Object> contrato) {
        return (Map<String, Object>) ((Map<String, Object>) contrato.get("components")).get("schemas");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> contrato() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONTRATO, StandardCharsets.UTF_8)) {
            return (Map<String, Object>) new Yaml().load(reader);
        }
    }
}
