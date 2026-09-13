package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El criterio de admisión del ADR-053 §5 para la columna que el backend#88 añadió al
 * metamodelo: <i>«el criterio de admisión de cualquier columna futura no es si es útil, sino
 * qué test falla cuando esté mal»</i>. Éste.
 *
 * <p>{@code rule_entity_type.api_collection_path} existe para que el 400 de
 * {@code POST /rule-entities} pueda decir a dónde ir. Una ruta mal escrita, o buena el día que
 * se sembró y renombrada después, convierte ese mensaje en una pista falsa — que es peor que no
 * dar ninguna, porque se sigue.</p>
 *
 * <p>Se compara contra el contrato y no contra los {@code @PostMapping} por lo que decidió el
 * backend#80: hay un solo contrato y lo que se sirve se declara en él, así que «está en el
 * contrato con un POST» es la misma frase que «existe y mis clientes la ven».</p>
 */
@TestSobreEsquemaReal
class RuleEntityTypeOwnEndpointIsRealTest {

    private static final Path CONTRACT = Path.of("openapi/personnel-administration-api.yaml");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyDeclaredCollectionPathIsAPostTheContractDeclares() {
        List<Map<String, Object>> declared = jdbcTemplate.queryForList("""
                select code, api_collection_path
                  from rulesystem.rule_entity_type
                 where api_collection_path is not null
                 order by code
                """);
        assertThat(declared)
                .as("hay al menos un tipo que declara su colección propia; si no, la columna sobra")
                .isNotEmpty();

        Map<String, Object> paths = contractPaths();

        Map<String, String> broken = new TreeMap<>();
        for (Map<String, Object> type : declared) {
            String path = (String) type.get("api_collection_path");
            Object operations = paths.get(path);
            if (operations == null) {
                broken.put((String) type.get("code"), path + " no existe en el contrato");
            } else if (!((Map<?, ?>) operations).containsKey("post")) {
                broken.put((String) type.get("code"), path + " existe en el contrato y no admite POST");
            }
        }

        assertThat(broken)
                .withFailMessage("""
                        Estos tipos declaran una colección propia que el contrato no sirve \
                        (tipo -> qué le pasa): %s

                        El 400 de POST /rule-entities manda ahí a quien se equivoca de puerta \
                        (backend#88). Si la ruta cambió, se sigue en una migración; si nunca \
                        existió, la declaración se pone a null, que es la forma de decir «no se \
                        sabe por dónde».
                        """, broken)
                .isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> contractPaths() {
        assertThat(CONTRACT).as("el contrato se lee desde la raíz del módulo").exists();
        try (Reader reader = Files.newBufferedReader(CONTRACT, StandardCharsets.UTF_8)) {
            Map<String, Object> document = new Yaml().load(reader);
            return (Map<String, Object>) document.get("paths");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
