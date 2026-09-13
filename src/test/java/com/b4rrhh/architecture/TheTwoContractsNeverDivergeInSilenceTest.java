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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un contrato, y lo que se sirve esta declarado en el.
 *
 * <p>Hubo dos. {@code payroll-api.yaml} nacio en abril de 2026 como banco de diseno del
 * ADR-029 y acabo describiendo endpoints servidos sin que nadie lo decidiera.
 * {@code OpenApiContractsAreValidTest} miraba que cada fichero fuese un documento OpenAPI
 * correcto, y dos ficheros pueden ser los dos correctos y contradecirse: eso es lo que dejo
 * {@code GET /payroll/calculation-runs/{runId}/messages} servido, documentado solo en el
 * pequeno, e invisible para el cliente que el frontend genera del grande
 * ({@code frontend#61}). Cuando se hizo el barrido entero resulto que <b>ninguno de los dos
 * describia las once rutas servidas</b>: no eran uno completo y otro parcial, eran dos
 * parciales que se solapaban en siete.
 *
 * <p>Se fusiono en {@code backend#80}. Este test es lo que impide que vuelva a pasar, y
 * comprueba tres cosas separadas para que el mensaje diga cual se rompio:
 *
 * <ol>
 *   <li><b>Lo que el backend sirve esta declarado.</b> Un campo que el backend manda y el
 *       contrato no declara no existe para el cliente generado, y nadie se entera hasta que
 *       alguien lo echa de menos en una pantalla. Asi se encontro que
 *       {@code PayrollResponse} servia cuatro campos que el contrato callaba, uno de ellos
 *       {@code statusReasonCode}, que es el registro del acto humano (ADR-059).</li>
 *   <li><b>Hay un solo contrato.</b> No es una cuenta de ficheros por gusto: mientras solo
 *       haya uno, "esta declarado" y "esta declarado donde mis clientes lo leen" son la
 *       misma frase. En cuanto haya dos, dejan de serlo.</li>
 *   <li><b>Y si algun dia vuelve a haber dos, ninguna operacion vive en los dos.</b> Esta
 *       era un inventario congelado de siete duplicados mientras se decidia cual mandaba;
 *       ahora que la fusion esta hecha, es la regla que entonces no se podia exigir.</li>
 * </ol>
 */
class TheTwoContractsNeverDivergeInSilenceTest {

    private static final Path CONTRATOS = Path.of("openapi");

    /** Del que generan su cliente el frontend y el designer. */
    private static final Path PUBLICADO = CONTRATOS.resolve("personnel-administration-api.yaml");

    private static final Path DTOS =
            Path.of("src/main/java/com/b4rrhh/payroll/infrastructure/web/dto");

    @Test
    void everyFieldTheBackendServesIsDeclaredInTheContractItsClientsGenerateFrom() {
        Map<String, Map<String, Object>> esquemas = esquemasDe(PUBLICADO);
        List<String> fallos = new ArrayList<>();

        for (Path dto : ficherosJava(DTOS)) {
            String nombre = dto.getFileName().toString().replace(".java", "");
            Map<String, Object> esquema = esquemas.get(nombre);
            if (esquema == null) {
                // Un DTO puede ser interno y no asomar por el API. Lo que no se acepta es que
                // este declarado a medias, que es lo que mira el bucle de abajo.
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
    void thereIsExactlyOneContract() {
        List<Path> contratos = contratos();

        assertEquals(List.of(PUBLICADO), contratos,
                "La fuente de verdad del API es un solo fichero desde backend#80. Si has anadido "
                        + "otro contrato, eso reabre justo el defecto que aquel issue cerro: dos "
                        + "ficheros que se contradicen sin que nada avise, y un cliente que genera "
                        + "de uno de los dos.\nSi de verdad hacen falta dos, lo que hay que hacer "
                        + "primero es decidir y escribir el papel de cada uno; luego este test se "
                        + "actualiza, y el de abajo se vuelve imprescindible.");
    }

    @Test
    void noOperationLivesInTwoContractsAtOnce() {
        Map<String, List<String>> contratosPorOperacion = new TreeMap<>();
        for (Path contrato : contratos()) {
            operacionesDe(contrato).keySet().forEach(operacion ->
                    contratosPorOperacion
                            .computeIfAbsent(operacion, clave -> new ArrayList<>())
                            .add(contrato.getFileName().toString()));
        }

        Map<String, List<String>> duplicadas = new TreeMap<>();
        contratosPorOperacion.forEach((operacion, ficheros) -> {
            if (ficheros.size() > 1) {
                duplicadas.put(operacion, ficheros);
            }
        });

        assertEquals(Map.of(), duplicadas,
                "Hay operaciones escritas en mas de un contrato. Cada una tiene que vivir en uno "
                        + "solo: dos copias del mismo endpoint derivan, y la que derive sera la que "
                        + "tu cliente no genera.\nEsto fue un inventario congelado de siete mientras "
                        + "backend#80 decidia cual mandaba. Ya esta decidido, asi que ahora la lista "
                        + "tiene que estar vacia.");
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

    /** Todos los .yaml de openapi/, no una lista fija: un contrato nuevo tiene que saltar aqui. */
    private static List<Path> contratos() {
        return ficheros(CONTRATOS, ".yaml");
    }

    private static List<Path> ficherosJava(Path directorio) {
        return ficheros(directorio, ".java");
    }

    private static List<Path> ficheros(Path directorio, String extension) {
        assertTrue(Files.isDirectory(directorio), "No encuentro " + directorio.toAbsolutePath());
        try (Stream<Path> ficheros = Files.list(directorio)) {
            return ficheros
                    .filter(f -> f.getFileName().toString().endsWith(extension))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
