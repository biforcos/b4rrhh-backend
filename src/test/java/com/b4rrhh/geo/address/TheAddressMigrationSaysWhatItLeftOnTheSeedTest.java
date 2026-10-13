package com.b4rrhh.geo.address;

import java.util.TreeMap;
import java.util.Objects;
import java.util.Map;
import com.b4rrhh.geo.territory.infrastructure.persistence.PostalCodeJdbcAdapter;
import com.b4rrhh.geo.address.domain.model.PostalAddressIssue;
import com.b4rrhh.geo.address.application.service.PostalAddressValidator;
import com.b4rrhh.geo.address.domain.model.PostalAddressInput;
import com.b4rrhh.geo.territory.infrastructure.persistence.MunicipalityJdbcAdapter;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La V178 dice en su cabecera cuántas direcciones de la semilla quedaron normalizadas y cuántas no, y
 * por qué motivo (backend#155). Este test lo comprueba contra una copia de la semilla migrada, y
 * comprueba además que la validación de Java, la que corre en cada alta, da de cada fila el mismo
 * motivo que dio la migración. (La V177 también trae cifras en su cabecera: eran las de la regla
 * floja, la que no miraba si el CP existe, y se quedan como historia.)
 *
 * <p>Sólo corre si se le da la base: {@code -Db4rrhh.seed.url=jdbc:postgresql://localhost:5432/<copia>}
 * (usuario y contraseña {@code b4rrhh}). En la integración continua no hay semilla, y entonces se salta
 * diciéndolo, que no es lo mismo que pasar. La copia se hace así: {@code create database X template
 * b4rrhh_semilla}, y un backend del árbol arrancado contra ella la migra.</p>
 */
class TheAddressMigrationSaysWhatItLeftOnTheSeedTest {

    private static final Path MIGRACION = Path.of(
            "src/main/resources/db/migration/V178__an_address_is_normalized_only_with_a_postal_code_of_the_master_and_says_why_not.sql");

    private static JdbcTemplate seed;

    @BeforeAll
    static void connect() {
        String url = System.getProperty("b4rrhh.seed.url");
        assumeTrue(url != null && !url.isBlank(),
                "Sin -Db4rrhh.seed.url no hay semilla contra la que comprobar la cabecera de la V178");
        // Una sola conexión: la comprobación fila a fila hace miles de consultas.
        seed = new JdbcTemplate(new SingleConnectionDataSource(url, "b4rrhh", "b4rrhh", true));
    }

    @Test
    void theHeaderOfTheMigrationSaysWhatItLeftOnTheSeed() throws IOException {
        String header = Files.readString(MIGRACION, StandardCharsets.UTF_8);
        for (String[] tabla : new String[][] {
                {"employee.address", "direcciones"},
                {"rulesystem.company_profile", "perfiles"},
                {"rulesystem.work_center_profile", "perfiles"}}) {
            Matcher m = Pattern.compile(Pattern.quote(tabla[0]) + ":\\s+(\\d+) " + tabla[1]
                    + ",\\s+(\\d+) normalizad[oa]s y (\\d+) sin normalizar").matcher(header);
            assertThat(m.find()).as("la cabecera de la V178 dice cuántas de %s", tabla[0]).isTrue();

            Integer total = seed.queryForObject("select count(*) from " + tabla[0], Integer.class);
            Integer normalizadas = seed.queryForObject("select count(*) from " + tabla[0] + " where normalized", Integer.class);
            assertThat(List.of(total, normalizadas, total - normalizadas))
                    .as("%s: total, normalizadas y sin normalizar en la semilla", tabla[0])
                    .containsExactly(Integer.valueOf(m.group(1)), Integer.valueOf(m.group(2)), Integer.valueOf(m.group(3)));
        }

        // El desglose por motivo: «--   employee.address CP_DESCONOCIDO: 1026».
        Map<String, Integer> enLaCabecera = new TreeMap<>();
        Matcher motivo = Pattern.compile("--\\s+(\\w+\\.\\w+) ([A-Z_]+): (\\d+)").matcher(header);
        while (motivo.find()) {
            enLaCabecera.put(motivo.group(1) + " " + motivo.group(2), Integer.valueOf(motivo.group(3)));
        }
        Map<String, Integer> enLaBase = new TreeMap<>();
        for (String tabla : List.of("employee.address", "rulesystem.company_profile", "rulesystem.work_center_profile")) {
            seed.query("select normalization_issue, count(*) from " + tabla
                    + " where normalization_issue is not null group by 1", rs -> {
                enLaBase.put(tabla + " " + rs.getString(1), rs.getInt(2));
            });
        }
        assertThat(enLaCabecera).as("los motivos de la cabecera de la V178 contra los de la semilla").isEqualTo(enLaBase);
    }

    @Test
    void theJavaValidationSaysOfEveryRowWhatTheMigrationSaid() {
        PostalAddressValidator validator = new PostalAddressValidator(
                catalogFromTheSeed(), new MunicipalityJdbcAdapter(seed), new PostalCodeJdbcAdapter(seed));
        List<String> discrepan = new ArrayList<>();
        for (String[] fuente : new String[][] {
                {"employee.address a join employee.employee e on e.id = a.employee_id", "e.rule_system_code", "a.start_date"},
                {"rulesystem.company_profile a join rulesystem.rule_entity re on re.id = a.company_rule_entity_id"
                        + " join rulesystem.rule_system_layer rsl on rsl.layer_code = re.layer_code", "rsl.rule_system_code", "current_date"},
                {"rulesystem.work_center_profile a join rulesystem.rule_entity re on re.id = a.work_center_rule_entity_id"
                        + " join rulesystem.rule_system_layer rsl on rsl.layer_code = re.layer_code", "rsl.rule_system_code", "current_date"}}) {
            seed.query("select a.id, " + fuente[1] + ", " + fuente[2] + ", a.street_type_code, a.street_name, a.street_number,"
                    + " a.number_qualifier, a.block, a.portal, a.stair, a.floor, a.door, a.locality, a.municipality_code,"
                    + " a.subdivision_code, a.postal_code, a.country_code, a.normalization_issue from " + fuente[0]
                    + " where a.country_code is not null", rs -> {
                PostalAddressInput input = new PostalAddressInput(rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11),
                        rs.getString(12), rs.getString(13), rs.getString(14), rs.getString(15), rs.getString(16),
                        rs.getString(17));
                PostalAddressIssue issue = validator.validate(rs.getString(2), input, rs.getObject(3, LocalDate.class))
                        .normalizationIssue();
                String java = issue == null ? null : issue.name();
                if (!Objects.equals(java, rs.getString(18))) {
                    discrepan.add(fuente[0].split(" ")[0] + "#" + rs.getLong(1) + ": migración " + rs.getString(18) + ", Java " + java);
                }
            });
        }
        assertThat(discrepan).as("filas en las que la migración y PostalAddressValidator no dan el mismo motivo").isEmpty();
    }

    /** El catálogo leído de la semilla: sólo las dos preguntas que hace la validación. */
    private static RuleEntityRepository catalogFromTheSeed() {
        RuleEntityRepository catalog = mock(RuleEntityRepository.class);
        when(catalog.findByBusinessKey(anyString(), eq("COUNTRY"), anyString())).thenAnswer(call -> seed.query("""
                select re.code, re.layer_code, re.start_date, re.end_date, re.active
                  from rulesystem.rule_entity re
                  join rulesystem.rule_system_layer rsl on rsl.layer_code = re.layer_code
                 where rsl.rule_system_code = ? and re.rule_entity_type_code = 'COUNTRY' and re.code = ?
                """, (rs, n) -> entity(rs.getString(1), rs.getString(2), "COUNTRY", rs.getObject(3, LocalDate.class),
                rs.getObject(4, LocalDate.class), rs.getBoolean(5)), call.getArgument(0), call.getArgument(2))
                .stream().findFirst());
        when(catalog.findInLayerByBusinessKey(anyString(), anyString(), anyString())).thenAnswer(call -> seed.query("""
                select re.code, re.layer_code, re.rule_entity_type_code, re.start_date, re.end_date, re.active
                  from rulesystem.rule_entity re
                 where re.layer_code = ? and re.rule_entity_type_code = ? and re.code = ?
                """, (rs, n) -> entity(rs.getString(1), rs.getString(2), rs.getString(3), rs.getObject(4, LocalDate.class),
                rs.getObject(5, LocalDate.class), rs.getBoolean(6)), call.getArgument(0), call.getArgument(1), call.getArgument(2))
                .stream().findFirst());
        return catalog;
    }

    private static RuleEntity entity(String code, String layer, String type, LocalDate start, LocalDate end, boolean active) {
        return new RuleEntity(null, null, layer, null, type, code, code, null, active, start, end, null, null);
    }
}
