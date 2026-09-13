package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dos contratos describen la nomina, y hasta ahora nada comprobaba que dijeran lo mismo.
 *
 * OpenApiContractsAreValidTest mira que cada fichero sea un documento OpenAPI correcto. Dos
 * ficheros pueden ser los dos correctos y contradecirse, y es exactamente lo que pasaba:
 * GET /payroll/calculation-runs/{runId}/messages estaba servido, documentado en
 * payroll-api.yaml y ausente de personnel-administration-api.yaml, que es del unico del que
 * el frontend genera su cliente. Un endpoint servido e invisible (frontend#61).
 *
 * Este test es lo que convierte esa deriva en un fallo de la suite. Comprueba tres cosas
 * distintas, separadas para que el mensaje diga cual se rompio:
 *
 * 1. Que lo que el backend sirve esta escrito en el contrato del que sus clientes generan.
 *    Es la regla que de verdad importa: un campo que el backend manda y el contrato no
 *    declara no existe para el cliente generado, y nadie se entera hasta que alguien lo echa
 *    de menos en una pantalla.
 *
 * 2. Que la superficie compartida por los dos contratos no crece. Hoy son siete operaciones;
 *    el dia que alguien escriba una octava en los dos sitios, esto falla y le obliga a decidir
 *    en cual va, en vez de dejar dos copias sueltas.
 *
 * 3. Que la deriva conocida entre las copias no crece. La lista de abajo es el inventario de lo
 *    que hoy ya diverge, y es a proposito una lista congelada y no una regla: lo que hay que
 *    hacer con cada pareja —cual manda y cual sobra— es una decision de modelo que esta
 *    pendiente en backend#80. Hasta que se tome, lo unico que se puede exigir es que no
 *    empeore. El test falla en los dos sentidos: si aparece una pareja nueva que diverge, y
 *    tambien si una deja de divergir sin quitarla de la lista. Asi el inventario solo puede
 *    encoger a proposito.
 *
 * Cuando backend#80 se cierre, los dos inventarios se quedaran vacios y los tests 2 y 3 pasaran
 * a ser la regla que hoy no se puede exigir: que ninguna operacion viva en los dos contratos a
 * la vez.
 */
class TheTwoContractsNeverDivergeInSilenceTest {

    /** Del que generan su cliente el frontend y el designer. */
    private static final Path PUBLICADO = Path.of("openapi/personnel-administration-api.yaml");

    private static final Path NOMINA = Path.of("openapi/payroll-api.yaml");

    private static final Path DTOS =
            Path.of("src/main/java/com/b4rrhh/payroll/infrastructure/web/dto");

    /**
     * Las siete operaciones que hoy estan escritas en los dos contratos. Cada entrada es
     * "METODO ruta". Dos de ellas ademas llevan distinto operationId en cada fichero, o sea que
     * el metodo del cliente generado se llama de una forma u otra segun de cual generes:
     * getPayrollCalculationRun / getPayrollCalculationRunById, y
     * bulkInvalidatePayroll / invalidatePayrollBulk.
     */
    private static final Set<String> SUPERFICIE_COMPARTIDA_CONOCIDA = new TreeSet<>(Set.of(
            "GET /payroll/calculation-runs/{runId}",
            "GET /payroll/calculation-runs/{runId}/messages",
            "GET /payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}"
                    + "/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}",
            "POST /payroll/calculation-runs/launch",
            "POST /payrolls/invalidate-bulk",
            "POST /payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}"
                    + "/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/invalidate",
            "POST /payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}"
                    + "/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/validate"
    ));

    /**
     * Los esquemas que existen en los dos contratos con el mismo nombre y distinto conjunto de
     * propiedades. Hoy solo uno, y es el grave: PayrollResponse tiene en cada fichero campos que
     * el otro no tiene, y el backend sirve la union de los dos.
     *
     * Los otros trece nombres compartidos coinciden en que campos llevan y difieren solo en el
     * detalle —descripciones, format: int32, un enum escrito a mano en un sitio y referenciado en
     * el otro—. Eso no se congela aqui porque no cambia la forma de los datos; se deja anotado en
     * el issue.
     */
    private static final Set<String> ESQUEMAS_QUE_YA_DIVERGEN = new TreeSet<>(Set.of(
            "PayrollResponse"
    ));

    @Test
    void everyFieldTheBackendServesIsDeclaredInTheContractItsClientsGenerateFrom() {
        Map<String, Map<String, Object>> esquemas = esquemasDe(PUBLICADO);
        List<String> fallos = new ArrayList<>();

        for (Path dto : ficheros(DTOS)) {
            String nombre = dto.getFileName().toString().replace(".java", "");
            Map<String, Object> esquema = esquemas.get(nombre);
            if (esquema == null) {
                // No todo DTO tiene que estar publicado: los hay que solo viven en payroll-api.yaml
                // mientras esa superficie no se publique. Lo que no se acepta es que este publicado
                // a medias, que es lo que mira el bucle de abajo.
                continue;
            }
            Set<String> declaradas = propiedadesDe(esquema, esquemas);
            for (String campo : componentesDelRecord(dto)) {
                if (!declaradas.contains(campo)) {
                    fallos.add(nombre + "." + campo);
                }
            }
        }

        assertTrue(fallos.isEmpty(),
                "El backend sirve campos que " + PUBLICADO + " no declara, y ese es el contrato del "
                        + "que el frontend y el designer generan su cliente: para ellos esos campos "
                        + "no existen.\n  " + String.join("\n  ", fallos)
                        + "\n\nSe arregla escribiendolos en el contrato, no quitandolos del DTO.");
    }

    @Test
    void theSurfaceWrittenInBothContractsDoesNotGrow() {
        Set<String> compartida = new TreeSet<>(operacionesDe(PUBLICADO).keySet());
        compartida.retainAll(operacionesDe(NOMINA).keySet());

        assertEquals(SUPERFICIE_COMPARTIDA_CONOCIDA, compartida,
                "Ha cambiado que operaciones estan escritas en los dos contratos a la vez.\n"
                        + "Si has anadido una: decide en cual va y escribela solo ahi. Mientras "
                        + "backend#80 no diga cual es el papel de cada fichero, la unica regla que "
                        + "se puede exigir es que la duplicacion no crezca.\n"
                        + "Si has quitado una: quitala tambien de SUPERFICIE_COMPARTIDA_CONOCIDA, "
                        + "para que el inventario diga la verdad.");
    }

    @Test
    void theKnownDivergenceBetweenTheSharedSchemasDoesNotGrow() {
        Map<String, Map<String, Object>> publicados = esquemasDe(PUBLICADO);
        Map<String, Map<String, Object>> nomina = esquemasDe(NOMINA);

        Set<String> divergen = new TreeSet<>();
        for (String nombre : publicados.keySet()) {
            if (!nomina.containsKey(nombre)) {
                continue;
            }
            if (!propiedadesDe(publicados.get(nombre), publicados)
                    .equals(propiedadesDe(nomina.get(nombre), nomina))) {
                divergen.add(nombre);
            }
        }

        assertEquals(ESQUEMAS_QUE_YA_DIVERGEN, divergen,
                "Ha cambiado que esquemas llevan el mismo nombre en los dos contratos y distintos "
                        + "campos.\nSi hay uno nuevo, es un defecto vivo: el mismo nombre describe "
                        + "dos formas, y quien lea un contrato u otro construira cosas distintas.\n"
                        + "Si has arreglado uno, quitalo de ESQUEMAS_QUE_YA_DIVERGEN.");
    }

    // ---- lectura de los contratos y de los DTO ----

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> esquemasDe(Path contrato) {
        Map<String, Object> raiz = (Map<String, Object>) cargar(contrato);
        Map<String, Object> componentes = (Map<String, Object>) raiz.get("components");
        if (componentes == null) {
            return Map.of();
        }
        Map<String, Object> esquemas = (Map<String, Object>) componentes.get("schemas");
        if (esquemas == null) {
            return Map.of();
        }
        Map<String, Map<String, Object>> salida = new LinkedHashMap<>();
        esquemas.forEach((nombre, cuerpo) -> {
            if (cuerpo instanceof Map<?, ?> mapa) {
                salida.put(nombre, (Map<String, Object>) mapa);
            }
        });
        return salida;
    }

    /** "METODO ruta" -> operationId, para todas las operaciones del contrato. */
    @SuppressWarnings("unchecked")
    private static Map<String, String> operacionesDe(Path contrato) {
        Map<String, Object> raiz = (Map<String, Object>) cargar(contrato);
        Map<String, Object> rutas = (Map<String, Object>) raiz.get("paths");
        Map<String, String> salida = new LinkedHashMap<>();
        if (rutas == null) {
            return salida;
        }
        Set<String> verbos = Set.of("get", "post", "put", "patch", "delete");
        rutas.forEach((ruta, cuerpo) -> {
            if (!(cuerpo instanceof Map<?, ?> mapa)) {
                return;
            }
            mapa.forEach((verbo, operacion) -> {
                if (verbos.contains(String.valueOf(verbo)) && operacion instanceof Map<?, ?> op) {
                    salida.put(String.valueOf(verbo).toUpperCase() + " " + ruta,
                            String.valueOf(((Map<String, Object>) op).get("operationId")));
                }
            });
        });
        return salida;
    }

    /** Las propiedades de un esquema, resolviendo los allOf y los $ref dentro del mismo contrato. */
    private static Set<String> propiedadesDe(Map<String, Object> esquema,
                                             Map<String, Map<String, Object>> contrato) {
        return propiedadesDe(esquema, contrato, new LinkedHashSet<>());
    }

    @SuppressWarnings("unchecked")
    private static Set<String> propiedadesDe(Map<String, Object> esquema,
                                             Map<String, Map<String, Object>> contrato,
                                             Set<String> visitados) {
        Set<String> salida = new TreeSet<>();
        if (esquema == null) {
            return salida;
        }
        Object ref = esquema.get("$ref");
        if (ref != null) {
            String referencia = String.valueOf(ref);
            String nombre = referencia.substring(referencia.lastIndexOf('/') + 1);
            if (visitados.add(nombre)) {
                salida.addAll(propiedadesDe(contrato.get(nombre), contrato, visitados));
            }
            return salida;
        }
        if (esquema.get("properties") instanceof Map<?, ?> propiedades) {
            propiedades.keySet().forEach(clave -> salida.add(String.valueOf(clave)));
        }
        if (esquema.get("allOf") instanceof List<?> partes) {
            for (Object parte : partes) {
                if (parte instanceof Map<?, ?> mapa) {
                    salida.addAll(propiedadesDe((Map<String, Object>) mapa, contrato, visitados));
                }
            }
        }
        return salida;
    }

    private static final Pattern CABECERA_DEL_RECORD =
            Pattern.compile("record\\s+\\w+\\s*\\((.*?)\\)\\s*\\{", Pattern.DOTALL);

    /**
     * Los nombres de los componentes de un record, leyendo el fuente. Se lee el .java y no se usa
     * reflexion a proposito, por lo mismo que en el resto de tests de arquitectura: sin
     * dependencias nuevas, y la regla se comprueba de un vistazo.
     */
    private static List<String> componentesDelRecord(Path fuente) {
        Matcher cabecera = CABECERA_DEL_RECORD.matcher(leer(fuente));
        if (!cabecera.find()) {
            return List.of();
        }
        List<String> salida = new ArrayList<>();
        for (String componente : cabecera.group(1).split(",")) {
            String limpio = componente.strip();
            if (limpio.isEmpty()) {
                continue;
            }
            String[] partes = limpio.split("\\s+");
            salida.add(partes[partes.length - 1]);
        }
        return salida;
    }

    private static Object cargar(Path contrato) {
        return new Yaml().load(leer(contrato));
    }

    private static String leer(Path fichero) {
        try {
            return Files.readString(fichero, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No puedo leer " + fichero, e);
        }
    }

    private static List<Path> ficheros(Path directorio) {
        assertTrue(Files.isDirectory(directorio), "No encuentro " + directorio.toAbsolutePath());
        try (Stream<Path> ficheros = Files.list(directorio)) {
            return ficheros
                    .filter(f -> f.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
