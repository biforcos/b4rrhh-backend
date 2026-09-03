package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant.CatalogColumnUsage;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant.RuleSystemSource;
import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Guardarraíl dirigido por los participantes (backend#43): ninguna fila del esquema {@code employee}
 * apunta a un código de catálogo que no exista en {@code rulesystem.rule_entity}.
 *
 * Hermano de {@code EveryCatalogColumnIsDeclaredOrExemptedTest} (backend#29): aquél comprueba que
 * toda columna {@code *_code} esté declarada; éste, que lo declarado cuadre con el dato. Juntos
 * cierran lo que el ADR-053 §5 exige: sólo entra en el metamodelo lo que una guardia pueda comprobar.
 *
 * La lista de columnas no se escribe aquí. Sale de {@code declaredUsages()} de cada participante,
 * el mismo {@code Map} que usa {@code countReferences}: un vertical nuevo entra en la guardia con
 * declararse, y una segunda lista que alguien tuviera que mantener sería justo el registro central
 * que el patrón del ADR-047 existe para evitar.
 *
 * No hay clave ajena, a propósito (ADR-055): estas columnas guardan el código, no el id, y una
 * clave ajena no distingue «no existe» de «ya no está vigente». De ahí las dos reglas de la
 * comprobación: un código sólo existe dentro de su {@code rule_system_code} —propio en la tabla o
 * heredado del empleado, según declare el participante— y se miran todas las filas, vigentes o no,
 * igual que {@code countReferences}. La vigencia no se juzga aquí.
 */
@TestSobreEsquemaReal
class EveryCatalogColumnPointsToAnExistingCodeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private List<RuleEntityUsageParticipant> participants;

    @Test
    void noRowInTheEmployeeSchemaPointsToACodeThatDoesNotExist() {
        assertThat(declaredUsages()).as("los participantes declaran columnas").isNotEmpty();

        List<Orphans> orphans = orphans();

        assertThat(orphans)
                .withFailMessage("""
                        Hay filas del esquema employee que apuntan a códigos que no existen en
                        rulesystem.rule_entity (columna, tipo de catálogo, filas y reglamentación/código):
                        %s

                        Un código sólo existe dentro de su reglamentación. Si el código es correcto, lo
                        que falta es darlo de alta en rule_entity (con la vigencia que le toque: aquí no
                        se juzga). Si es un residuo —de un fixture, de un script, de una migración—, hay
                        que corregir la fila. Los que nunca se limpian son los que acaban leyéndose mal
                        para siempre en el histórico.
                        """, describe(orphans))
                .isEmpty();
    }

    // La prueba de la guardia: una fila con un código inexistente aparece sola en el fallo, con su
    // columna, su tipo y el código. Va cerrada en 2018 a propósito: el histórico cuenta igual que
    // lo vigente. La fila va dentro de la transacción del test y se deshace con ella.
    @Test
    void aRowPointingToAMissingCodeIsCaughtNamingItsColumn() {
        Long employeeId = DatosDePrueba.empleado(jdbcTemplate);
        jdbcTemplate.update("""
                insert into employee.presence (employee_id, presence_number, company_code, entry_reason_code, start_date, end_date)
                values (?, 1, 'ES01', 'ZZ_PROBE', date '2018-01-01', date '2018-12-31')
                """, employeeId);

        List<Orphans> orphans = orphans();

        assertThat(orphans).singleElement().satisfies(found -> {
            assertThat(found.usage().qualifiedColumn()).isEqualTo("presence.entry_reason_code");
            assertThat(found.usage().ruleEntityTypeCode()).isEqualTo("EMPLOYEE_PRESENCE_ENTRY_REASON");
            assertThat(found.rows()).isEqualTo(1);
            assertThat(found.codes()).containsExactly(entry("ESP/ZZ_PROBE", 1L));
        });
        // Lo que se lee en el fallo: columna, tipo, cuántas y cuáles. Es lo que convierte el
        // fallo en una decisión: no es lo mismo un residuo de fixture que doscientos convenios.
        assertThat(describe(orphans)).isEqualTo(
                "  presence.entry_reason_code (EMPLOYEE_PRESENCE_ENTRY_REASON): 1 fila(s): ESP/ZZ_PROBE (1)");
    }

    // El ámbito por reglamentación: el mismo código existe en FRA y no en ESP, y la fila es de un
    // empleado de ESP. Una comprobación que mirase sólo el código daría verde con datos rotos.
    @Test
    void aCodeThatOnlyExistsInAnotherRuleSystemIsStillMissing() {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity (rule_system_code, rule_entity_type_code, code, name, active, start_date)
                values ('FRA', 'CONTACT_TYPE', 'ZZ_PROBE', 'Sonde', true, date '1900-01-01')
                """);
        Long employeeId = DatosDePrueba.empleado(jdbcTemplate);
        jdbcTemplate.update("""
                insert into employee.contact (employee_id, contact_type_code, contact_value)
                values (?, 'ZZ_PROBE', '600000000')
                """, employeeId);

        assertThat(orphans()).singleElement().satisfies(found -> {
            assertThat(found.usage().qualifiedColumn()).isEqualTo("contact.contact_type_code");
            assertThat(found.usage().ruleSystemSource()).isEqualTo(RuleSystemSource.OWNER_EMPLOYEE);
            assertThat(found.codes()).containsExactly(entry("ESP/ZZ_PROBE", 1L));
        });
    }

    // Las dos tablas que llevan su propio rule_system_code no pasan por employee_id: la guardia las
    // acota por su columna, como declara el participante.
    @Test
    void aTableWithItsOwnRuleSystemCodeIsScopedByThatColumn() {
        jdbcTemplate.update("""
                insert into employee.employee_payroll_input
                    (rule_system_code, employee_type_code, employee_number, concept_code, period, quantity)
                values ('ESP', 'ZZ_PROBE', 'T00000001', 'TST_CONCEPT', 202601, 1)
                """);

        assertThat(orphans()).singleElement().satisfies(found -> {
            assertThat(found.usage().qualifiedColumn()).isEqualTo("employee_payroll_input.employee_type_code");
            assertThat(found.usage().ruleSystemSource()).isEqualTo(RuleSystemSource.OWN_COLUMN);
            assertThat(found.codes()).containsExactly(entry("ESP/ZZ_PROBE", 1L));
        });
    }

    /** Una columna con filas huérfanas: cuántas, y cada {@code reglamentación/código} con su recuento. */
    private record Orphans(CatalogColumnUsage usage, long rows, Map<String, Long> codes) {
    }

    private List<Orphans> orphans() {
        return declaredUsages().stream()
                .map(this::orphansOf)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingLong(Orphans::rows).reversed()
                        .thenComparing(orphans -> orphans.usage().qualifiedColumn()))
                .toList();
    }

    // Tabla, columna y tipo vienen del participante —constantes del vertical—, igual que en
    // countReferences; el único parámetro de la petición es el tipo de catálogo.
    private Optional<Orphans> orphansOf(CatalogColumnUsage usage) {
        String ruleSystemCode = switch (usage.ruleSystemSource()) {
            case OWN_COLUMN -> "owned.rule_system_code";
            case OWNER_EMPLOYEE -> "e.rule_system_code";
        };
        String join = usage.ruleSystemSource() == RuleSystemSource.OWNER_EMPLOYEE
                ? " join employee.employee e on e.id = owned.employee_id"
                : "";
        String sql = "select " + ruleSystemCode + " as rule_system_code, owned." + usage.column() + " as code, count(*) as n"
                + " from employee." + usage.table() + " owned" + join
                + " where owned." + usage.column() + " is not null"
                + "   and not exists (select 1 from rulesystem.rule_entity re"
                + "                    where re.rule_system_code = " + ruleSystemCode
                + "                      and re.rule_entity_type_code = ?"
                + "                      and re.code = owned." + usage.column() + ")"
                + " group by 1, 2 order by 3 desc, 1, 2";

        Map<String, Long> codes = new LinkedHashMap<>();
        jdbcTemplate.query(sql, row -> {
            codes.put(row.getString("rule_system_code") + "/" + row.getString("code"), row.getLong("n"));
        }, usage.ruleEntityTypeCode());

        if (codes.isEmpty()) {
            return Optional.empty();
        }
        long rows = codes.values().stream().mapToLong(Long::longValue).sum();
        return Optional.of(new Orphans(usage, rows, codes));
    }

    private Set<CatalogColumnUsage> declaredUsages() {
        return participants.stream()
                .filter(EmployeeOwnedRuleEntityUsageParticipant.class::isInstance)
                .map(EmployeeOwnedRuleEntityUsageParticipant.class::cast)
                .flatMap(participant -> participant.declaredUsages().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String describe(List<Orphans> orphans) {
        return orphans.stream()
                .map(found -> "  " + found.usage().qualifiedColumn()
                        + " (" + found.usage().ruleEntityTypeCode() + "): "
                        + found.rows() + " fila(s): "
                        + found.codes().entrySet().stream()
                                .map(code -> code.getKey() + " (" + code.getValue() + ")")
                                .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }
}
