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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Los tipos universales salen del modelo, no de una lista a mano: son los de clase
     * «vocabulario del dominio» (ADR-054 §8, V111). CONTRACT, CONTRACT_SUBTYPE, AGREEMENT y
     * AGREEMENT_CATEGORY quedan fuera solos —son citas de la norma y sus literales *deben*
     * diferir entre reglamentaciones—, igual que los nombres propios.
     */
    private List<String> universalTypeCodes() {
        return jdbcTemplate.queryForList("""
                select code from rulesystem.rule_entity_type
                 where literal_class = 'DOMAIN_VOCABULARY'
                 order by code
                """, String.class);
    }

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

    // backend#25: la guardia de arriba ve la divergencia pero no la ausencia. Para un tipo
    // universal, faltar en una reglamentación activa rompe la universalidad igual que decir
    // otra cosa: si HIRING es lo mismo en todas partes, no puede estar sólo en una. Hoy la
    // universalidad sale del cross join de las semillas —no del modelo— y desaparece con la
    // primera fila insertada a mano.
    @Test
    void universalCodesExistInEveryActiveRuleSystem() {
        List<Absence> absences = findAbsences();

        assertThat(absences)
                .withFailMessage(() -> describeAbsences(absences))
                .isEmpty();
    }

    // La prueba de la guardia: se borra un código de una sola reglamentación —dentro de la
    // transacción del test, que se deshace sola— y el mensaje tiene que decir en cuáles está
    // y en cuál falta, no sólo que falta en alguna.
    @Test
    void reportsWhereTheCodeIsAndWhereItIsMissingWhenItDisappearsFromOneRuleSystem() {
        int deleted = jdbcTemplate.update("""
                delete from rulesystem.rule_entity
                 where rule_system_code = 'PRT'
                   and rule_entity_type_code = 'EMPLOYEE_PRESENCE_ENTRY_REASON'
                   and code = 'HIRING'
                """);
        assertThat(deleted).as("HIRING/PRT viene en la semilla").isEqualTo(1);

        List<Absence> absences = findAbsences();

        // Se afirma contenido, no tamaño: un hasSize ata la sonda al estado global de la
        // semilla y la pone roja por causas ajenas a HIRING, que ya vigila el otro test.
        String message = describeAbsences(absences);
        assertThat(message).contains(
                "EMPLOYEE_PRESENCE_ENTRY_REASON/HIRING no está en todas las reglamentaciones activas"
                        + " — está en: ESP, FRA — falta en: PRT");
    }

    /** Un código universal cuyas filas no dicen lo mismo en todas las reglamentaciones. */
    private record Divergence(String typeCode, String code, List<Literal> literals) {
    }

    private record Literal(String ruleSystemCode, String name, String description) {
    }

    private List<Divergence> findDivergences() {
        List<String> universalTypeCodes = universalTypeCodes();
        String placeholders = universalTypeCodes.stream().map(code -> "?").collect(Collectors.joining(", "));
        Object[] params = universalTypeCodes.toArray();

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

    /** Un código universal que no existe en todas las reglamentaciones activas. */
    private record Absence(String typeCode, String code, List<String> presentIn, List<String> missingIn) {
    }

    private List<Absence> findAbsences() {
        List<String> activeRuleSystems = jdbcTemplate.queryForList(
                "select code from rulesystem.rule_system where active order by code", String.class);

        // Un tipo cerrado sólo se declara donde hace falta: que EMPLOYEE_TYPE exista sólo
        // en ESP no es un hueco (ADR-054, Consecuencias). En el chequeo de divergencia sí
        // entra: donde exista, tiene que decir lo mismo.
        List<String> checkedTypeCodes = jdbcTemplate.queryForList("""
                select code from rulesystem.rule_entity_type
                 where literal_class = 'DOMAIN_VOCABULARY'
                   and maintenance_mode <> 'CLOSED'
                 order by code
                """, String.class);

        String placeholders = checkedTypeCodes.stream().map(code -> "?").collect(Collectors.joining(", "));
        Object[] params = new Object[checkedTypeCodes.size() + 1];
        checkedTypeCodes.toArray(params);
        params[checkedTypeCodes.size()] = activeRuleSystems.size();

        return jdbcTemplate.query("""
                select rule_entity_type_code, code
                  from rulesystem.rule_entity
                 where rule_entity_type_code in (%s)
                   and rule_system_code in (select code from rulesystem.rule_system where active)
                 group by rule_entity_type_code, code
                having count(distinct rule_system_code) <> ?
                 order by rule_entity_type_code, code
                """.formatted(placeholders),
                        (rs, i) -> new Absence(rs.getString(1), rs.getString(2), List.of(), List.of()),
                        params)
                .stream()
                .map(absence -> {
                    List<String> presentIn = jdbcTemplate.queryForList("""
                            select distinct rule_system_code
                              from rulesystem.rule_entity
                             where rule_entity_type_code = ?
                               and code = ?
                             order by rule_system_code
                            """, String.class, absence.typeCode(), absence.code());
                    List<String> missingIn = activeRuleSystems.stream()
                            .filter(ruleSystem -> !presentIn.contains(ruleSystem))
                            .toList();
                    return new Absence(absence.typeCode(), absence.code(), presentIn, missingIn);
                })
                .toList();
    }

    private static String describeAbsences(List<Absence> absences) {
        return absences.stream()
                .map(absence -> absence.typeCode() + "/" + absence.code()
                        + " no está en todas las reglamentaciones activas — está en: "
                        + String.join(", ", absence.presentIn())
                        + " — falta en: " + String.join(", ", absence.missingIn()))
                .collect(Collectors.joining("\n",
                        "Códigos universales ausentes de alguna reglamentación activa (ADR-052, backend#25):\n", ""));
    }
}
