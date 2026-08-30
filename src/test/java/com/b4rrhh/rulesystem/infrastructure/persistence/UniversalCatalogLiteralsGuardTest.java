package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardia del ADR-052 (backend#22): un tipo de catálogo que es vocabulario del dominio —y no una
 * cita de la norma de un país— tiene el mismo literal en todas las reglamentaciones. Hoy el modelo
 * no lo garantiza: {@code HIRING/ESP}, {@code HIRING/FRA} y {@code HIRING/PRT} son tres filas
 * independientes que sólo la convención mantiene iguales, y las convenciones se rompen en
 * silencio porque no hay pantalla donde compararlas. Este test es esa pantalla.
 *
 * El arreglo de fondo (catálogos universales sin {@code rule_system_code}, ADR-052 §7) no se hace
 * aquí: toca la identidad de la tabla central del metamodelo.
 */
@TestSobreEsquemaReal
class UniversalCatalogLiteralsGuardTest {

    /**
     * Los tipos universales, **provisionalmente** aquí. El ADR-052 §2 los declara como un
     * atributo de {@code rule_entity_type} que llegará con el #15; cuando exista, esta lista se
     * sustituye por una consulta a ese atributo y este comentario desaparece.
     *
     * {@code CONTACT_TYPE} es el nombre actual del tipo de contacto: V37 canonicalizó
     * {@code EMPLOYEE_CONTACT_TYPE} bajo ese código.
     *
     * NO van aquí {@code CONTRACT}, {@code CONTRACT_SUBTYPE} ni {@code AGREEMENT_CATEGORY}: son
     * citas de la norma española y sus literales *deben* diferir entre reglamentaciones. Con
     * ellos dentro el test fallaría siempre y acabaría desactivado.
     */
    private static final List<String> UNIVERSAL_TYPE_CODES = List.of(
            "EMPLOYEE_PRESENCE_ENTRY_REASON",
            "EMPLOYEE_PRESENCE_EXIT_REASON",
            "EMPLOYEE_ADDRESS_TYPE",
            "CONTACT_TYPE",
            "EMPLOYEE_IDENTIFIER_TYPE"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void universalCodesReadTheSameInEveryRuleSystem() {
        List<Divergence> divergences = findDivergences();

        assertThat(divergences)
                .withFailMessage(() -> describe(divergences))
                .isEmpty();
    }

    // La prueba de la guardia: se cambia a mano un literal —dentro de la transacción del test,
    // que se deshace sola— y el mensaje tiene que decir qué código y qué reglamentaciones.
    @Test
    void reportsTheCodeTheRuleSystemsAndBothTextsWhenALiteralDrifts() {
        int changed = jdbcTemplate.update("""
                update rulesystem.rule_entity
                   set name = 'Embauche'
                 where rule_system_code = 'FRA'
                   and rule_entity_type_code = 'EMPLOYEE_PRESENCE_ENTRY_REASON'
                   and code = 'HIRING'
                """);
        assertThat(changed).as("HIRING/FRA viene en la semilla").isEqualTo(1);

        List<Divergence> divergences = findDivergences();

        assertThat(divergences).hasSize(1);
        String message = describe(divergences);
        assertThat(message)
                .contains("EMPLOYEE_PRESENCE_ENTRY_REASON")
                .contains("HIRING")
                .contains("FRA: name=\"Embauche\"")
                .contains("ESP: name=\"Hiring\"")
                .contains("PRT: name=\"Hiring\"");
    }

    /** Un código universal cuyas filas no dicen lo mismo en todas las reglamentaciones. */
    private record Divergence(String typeCode, String code, List<Literal> literals) {
    }

    private record Literal(String ruleSystemCode, String name, String description) {
    }

    private List<Divergence> findDivergences() {
        String placeholders = UNIVERSAL_TYPE_CODES.stream().map(code -> "?").collect(Collectors.joining(", "));
        Object[] params = UNIVERSAL_TYPE_CODES.toArray();

        List<Divergence> divergences = jdbcTemplate.query("""
                select rule_entity_type_code, code
                  from rulesystem.rule_entity
                 where rule_entity_type_code in (%s)
                 group by rule_entity_type_code, code
                having count(distinct name) > 1
                    or count(distinct coalesce(description, '')) > 1
                 order by rule_entity_type_code, code
                """.formatted(placeholders),
                (rs, i) -> new Divergence(rs.getString(1), rs.getString(2), List.of()),
                params);

        return divergences.stream()
                .map(divergence -> new Divergence(
                        divergence.typeCode(),
                        divergence.code(),
                        jdbcTemplate.query("""
                                select rule_system_code, name, description
                                  from rulesystem.rule_entity
                                 where rule_entity_type_code = ?
                                   and code = ?
                                 order by rule_system_code
                                """,
                                (rs, i) -> new Literal(rs.getString(1), rs.getString(2), rs.getString(3)),
                                divergence.typeCode(), divergence.code())))
                .toList();
    }

    private static String describe(List<Divergence> divergences) {
        return divergences.stream()
                .map(divergence -> divergence.typeCode() + "/" + divergence.code()
                        + " no dice lo mismo en todas las reglamentaciones:\n"
                        + divergence.literals().stream()
                        .map(literal -> "    " + literal.ruleSystemCode()
                                + ": name=\"" + literal.name() + "\""
                                + ", description=\"" + (literal.description() == null ? "" : literal.description()) + "\"")
                        .collect(Collectors.joining("\n")))
                .collect(Collectors.joining("\n",
                        "Literales universales divergentes (ADR-052, backend#22):\n", ""));
    }
}
