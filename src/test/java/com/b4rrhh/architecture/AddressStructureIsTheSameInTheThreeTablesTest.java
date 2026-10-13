package com.b4rrhh.architecture;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La dirección es una estructura y no tres (backend#155, ADR-078): {@code employee.address},
 * {@code rulesystem.company_profile} y {@code rulesystem.work_center_profile} la guardan cada una en
 * sus columnas, pero las mismas, con el mismo tipo; y en el API hay un solo DTO de dirección de
 * entrada y uno de salida.
 *
 * <h2>Por qué hace falta</h2>
 *
 * <p>Las tres definiciones de antes se parecían y no eran iguales: el país era {@code varchar(3)} en el
 * empleado y {@code char(3)} en los perfiles, y la empresa tenía tres DTO de dirección propios. Nadie
 * lo decidió: fue derivando. Lo que se vigila es que no vuelva a derivar.</p>
 *
 * <h2>Lo que mira</h2>
 *
 * <ol>
 *   <li><b>El esquema real</b>: las columnas de {@link #COLUMNAS} existen en las tres tablas con el
 *       mismo tipo y la misma longitud. La nulabilidad no entra: el domicilio de un empleado tiene
 *       país siempre, y una empresa o un centro pueden no tener dirección.</li>
 *   <li><b>El Java</b>: ningún DTO web declara {@code postalCode} salvo los dos de {@code geo.address},
 *       y las excepciones con nombre y motivo.</li>
 *   <li><b>El contrato</b>: ningún schema declara {@code postalCode} salvo los dos, y las mismas
 *       excepciones.</li>
 * </ol>
 *
 * <p>Las excepciones valen en las dos direcciones: una que ya no hace falta también sale roja.</p>
 */
@TestSobreEsquemaReal
class AddressStructureIsTheSameInTheThreeTablesTest {

    /** Las columnas de una dirección (V177, y la V178 añade portal y el motivo). */
    private static final List<String> COLUMNAS = List.of(
            "street_type_code", "street_name", "street_number", "number_qualifier", "block", "portal", "stair",
            "floor", "door", "locality", "municipality_code", "subdivision_code", "postal_code", "country_code",
            "normalized", "normalization_issue");

    private static final List<String> TABLAS = List.of(
            "employee.address", "rulesystem.company_profile", "rulesystem.work_center_profile");

    /** Las que eran de la dirección de texto libre y no pueden volver. */
    private static final List<String> RETIRADAS = List.of("street", "city", "region_code");

    private static final Set<String> LOS_DTO = Set.of("PostalAddressRequest", "PostalAddressResponse");

    /** Quien declara postalCode sin ser una dirección, y por qué. */
    private static final Map<String, String> NO_SON_DIRECCIONES = Map.of(
            "PayrollCompanyProfileResponse",
            "la cabecera del recibo: la vía, la población y el CP ya compuestos para imprimir, no una dirección",
            "PayrollEmployeeProfileResponse",
            "la cabecera del recibo: la vía, la población y el CP ya compuestos para imprimir, no una dirección",
            "TerritoryPostalCodeResponse",
            "la consulta de un CP en el maestro geo (backend#154): el CP, su provincia y el municipio sugerido");

    private static final Path FUENTES = Path.of("src/main/java");
    private static final Path CONTRATO = Path.of("openapi/personnel-administration-api.yaml");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void theThreeTablesHaveTheSameAddressColumnsWithTheSameType() {
        Map<String, Map<String, String>> porTabla = new LinkedHashMap<>();
        for (String tabla : TABLAS) {
            String[] partes = tabla.split("\\.");
            Map<String, String> columnas = new TreeMap<>();
            jdbcTemplate.query("""
                    select column_name, data_type, character_maximum_length
                      from information_schema.columns
                     where table_schema = ? and table_name = ?
                    """, rs -> {
                columnas.put(rs.getString(1), rs.getString(2) + "(" + rs.getObject(3) + ")");
            }, partes[0], partes[1]);
            porTabla.put(tabla, columnas);
        }

        Map<String, String> referencia = direccion(porTabla.get(TABLAS.get(0)));
        assertThat(referencia.keySet()).as("las columnas de dirección de " + TABLAS.get(0))
                .containsExactlyInAnyOrderElementsOf(COLUMNAS);
        for (String tabla : TABLAS) {
            assertThat(direccion(porTabla.get(tabla)))
                    .as("las columnas de dirección de %s, con su tipo, frente a las de %s", tabla, TABLAS.get(0))
                    .isEqualTo(referencia);
            assertThat(porTabla.get(tabla).keySet())
                    .as("%s no vuelve a la dirección de texto libre", tabla)
                    .doesNotContainAnyElementsOf(RETIRADAS);
        }
    }

    @Test
    void noWebDtoDeclaresAnAddressButTheTwoOfGeo() {
        Pattern record = Pattern.compile("public record (\\w+)\\s*\\(([^)]*)\\)", Pattern.DOTALL);
        Set<String> declaran = new TreeSet<>();
        for (Path fichero : ficheros()) {
            String relativo = FUENTES.relativize(fichero).toString().replace('\\', '/');
            if (!relativo.contains("/infrastructure/web/dto/")) {
                continue;
            }
            Matcher m = record.matcher(leer(fichero));
            while (m.find()) {
                if (Pattern.compile("\\bString postalCode\\b").matcher(m.group(2)).find()) {
                    declaran.add(m.group(1));
                }
            }
        }
        comprobar(declaran, "un DTO de src/main/java");
    }

    @Test
    void noContractSchemaDeclaresAnAddressButTheTwo() {
        Set<String> declaran = new TreeSet<>();
        String esquema = null;
        boolean enComponentes = false;
        for (String linea : leer(CONTRATO).split("\n")) {
            if (linea.startsWith("components:")) {
                enComponentes = true;
            }
            if (!enComponentes) {
                continue;
            }
            Matcher nombre = Pattern.compile("^    (\\w+):\\s*$").matcher(linea);
            if (nombre.matches()) {
                esquema = nombre.group(1);
            } else if (esquema != null && linea.equals("        postalCode:")) {
                declaran.add(esquema);
            }
        }
        comprobar(declaran, "un schema del contrato");
    }

    private static void comprobar(Set<String> declaran, String donde) {
        Set<String> sobran = new TreeSet<>(declaran);
        sobran.removeAll(LOS_DTO);
        sobran.removeAll(NO_SON_DIRECCIONES.keySet());
        assertThat(sobran)
                .withFailMessage("""
                        Declara una dirección %s que no es PostalAddressRequest ni PostalAddressResponse: %s
                        La dirección se define una vez (backend#155): úsalos. Si no es una dirección, apúntalo en
                        NO_SON_DIRECCIONES con su motivo.""", donde, sobran)
                .isEmpty();
        assertThat(declaran).as("los dos de geo.address siguen ahí, " + donde).containsAll(LOS_DTO);
        assertThat(declaran).as("NO_SON_DIRECCIONES no se queda con nombres que ya no existen, " + donde)
                .containsAll(NO_SON_DIRECCIONES.keySet());
    }

    private static Map<String, String> direccion(Map<String, String> columnas) {
        return columnas.entrySet().stream()
                .filter(e -> COLUMNAS.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
    }

    private static List<Path> ficheros() {
        try (Stream<Path> todos = Files.walk(FUENTES)) {
            return todos.filter(fichero -> fichero.toString().endsWith(".java")).sorted().toList();
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
