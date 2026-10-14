package com.b4rrhh.architecture;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lo que la base exige en un alta, el contrato lo exige también (backend#167).
 *
 * <p>El alta de un centro sin empresa daba un 500: la V40 puso {@code company_code not null} y el
 * contrato seguía diciendo que era opcional, así que la petición pasaba la puerta y reventaba en el
 * {@code insert}. Este test cruza el esquema real con el contrato: para cada petición de alta y la
 * tabla donde cae, toda columna {@code not null} sin valor por omisión que la petición expone con su
 * nombre en camelCase tiene que estar en {@code required}.</p>
 *
 * <p>Lo que la petición no expone —claves, fechas de auditoría, lo que pone el servidor— no cuenta:
 * no hay campo que declarar. Una petición de alta nueva entra añadiendo su tabla a {@link #ALTAS}.</p>
 */
@TestSobreEsquemaReal
class WhatTheDatabaseRequiresTheCreationContractRequiresTest {

    private static final Path CONTRATO = Path.of("openapi/personnel-administration-api.yaml");

    /** Petición de alta del contrato → la tabla donde se guarda lo que trae. */
    private static final Map<String, String> ALTAS = new TreeMap<>(Map.ofEntries(
            Map.entry("CreateWorkCenterRequest", "rulesystem.work_center_profile"),
            Map.entry("CreateCompanyRequest", "rulesystem.company_profile"),
            Map.entry("CreateWorkCenterContactRequest", "rulesystem.work_center_contact"),
            Map.entry("CreateRuleSystemRequest", "rulesystem.rule_system"),
            Map.entry("CreateRuleEntityRequest", "rulesystem.rule_entity"),
            Map.entry("CreateRuleEntityTypeRequest", "rulesystem.rule_entity_type"),
            Map.entry("CreateContactRequest", "employee.contact"),
            Map.entry("CreateIdentifierRequest", "employee.identifier"),
            Map.entry("CreateContractRequest", "employee.contract"),
            Map.entry("CreateLaborClassificationRequest", "employee.labor_classification"),
            Map.entry("CreateWorkingTimeRequest", "employee.working_time"),
            Map.entry("CreatePresenceRequest", "employee.presence"),
            Map.entry("CreateExtraPaymentRegimeRequest", "employee.extra_payment_regime"),
            Map.entry("CreateEmployeePayrollInputRequest", "employee.employee_payroll_input"),
            Map.entry("CreateEmployeeTaxInformationRequest", "employee.employee_tax_information")
    ));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyNotNullColumnACreationRequestExposesIsRequiredInTheContract() throws IOException {
        Map<String, Object> schemas = schemas();
        List<String> sueltas = new ArrayList<>();

        ALTAS.forEach((peticion, tabla) -> {
            Map<?, ?> schema = (Map<?, ?>) schemas.get(peticion);
            assertThat(schema).as("%s está en el contrato", peticion).isNotNull();
            Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
            List<?> required = schema.get("required") == null ? List.of() : (List<?>) schema.get("required");
            assertThat(columnas(tabla)).as("%s existe y tiene columnas", tabla).isNotEmpty();

            for (String columna : obligatorias(tabla)) {
                String campo = camel(columna);
                if (properties.containsKey(campo) && !required.contains(campo)) {
                    sueltas.add(peticion + "." + campo + " es opcional y " + tabla + "." + columna + " es not null");
                }
            }
        });

        assertThat(sueltas).isEmpty();
    }

    private Set<String> columnas(String tabla) {
        String[] partes = tabla.split("\\.");
        return Set.copyOf(jdbcTemplate.queryForList("""
                select column_name from information_schema.columns
                 where table_schema = ? and table_name = ?
                """, String.class, partes[0], partes[1]));
    }

    private List<String> obligatorias(String tabla) {
        String[] partes = tabla.split("\\.");
        return jdbcTemplate.queryForList("""
                select column_name from information_schema.columns
                 where table_schema = ? and table_name = ?
                   and is_nullable = 'NO' and column_default is null
                 order by ordinal_position
                """, String.class, partes[0], partes[1]);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> schemas() throws IOException {
        LoaderOptions opciones = new LoaderOptions();
        opciones.setCodePointLimit(20 * 1024 * 1024);
        try (Reader lector = Files.newBufferedReader(CONTRATO, StandardCharsets.UTF_8)) {
            Map<String, Object> contrato = new Yaml(opciones).load(lector);
            return (Map<String, Object>) ((Map<String, Object>) contrato.get("components")).get("schemas");
        }
    }

    private static String camel(String columna) {
        StringBuilder salida = new StringBuilder();
        boolean mayuscula = false;
        for (char c : columna.toCharArray()) {
            if (c == '_') {
                mayuscula = true;
            } else {
                salida.append(mayuscula ? Character.toUpperCase(c) : c);
                mayuscula = false;
            }
        }
        return salida.toString();
    }
}
