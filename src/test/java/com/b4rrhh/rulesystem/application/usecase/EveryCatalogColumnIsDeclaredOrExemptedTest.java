package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardarraíl dirigido por el esquema (backend#29): cada columna {@code *_code} de las tablas
 * de {@code employee} está declarada en un participante de uso o exenta aquí con su motivo.
 *
 * El otro guardarraíl, {@code EveryCatalogVerticalDeclaresItsRuleEntityUsageTest}, mira nombres
 * de clase: comprueba que el participante exista, no que declare todas las columnas, y no ve
 * los verticales que no siguen la convención. Este lee el esquema real, así que le da igual
 * cómo se llamen las clases, y una tabla nueva con una columna de código aparece sola en el
 * fallo. Se quedan los dos: aquél falla con un mensaje más claro.
 *
 * Se mira {@code employee} porque es donde viven los datos que referencian catálogos por
 * texto. Los perfiles de {@code rulesystem} apuntan a {@code rule_entity(id)} con clave ajena
 * y los protege la base de datos.
 */
@TestSobreEsquemaReal
class EveryCatalogColumnIsDeclaredOrExemptedTest {

    /**
     * Columnas {@code *_code} que NO son un código de {@code rule_entity}, con el motivo. Una
     * línea por columna; una exención sin motivo no vale.
     */
    private static final Map<String, String> EXEMPT = Map.of(
            "employee.rule_system_code",
            "es la reglamentación misma (rulesystem.rule_system), no una fila de rule_entity",
            "employee_payroll_input.rule_system_code",
            "es la reglamentación misma (rulesystem.rule_system), no una fila de rule_entity",
            "employee_payroll_input.concept_code",
            "es un concepto del motor de nómina (payroll_engine.payroll_concept), no una fila de rule_entity",
            "address.postal_code",
            "es el código postal escrito por el usuario; no hay catálogo de códigos postales",
            "address.region_code",
            "es texto libre de provincia/región; no existe un tipo REGION en rule_entity_type"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private List<RuleEntityUsageParticipant> participants;

    @Test
    void everyCodeColumnInTheEmployeeSchemaIsDeclaredByAParticipantOrExemptedWithAReason() {
        assertThat(schemaColumns()).as("el esquema tiene columnas *_code").isNotEmpty();

        assertThat(undeclared())
                .withFailMessage("""
                        Estas columnas *_code del esquema employee no las declara ningún participante
                        de uso ni están exentas: %s

                        Si guardan un código de rule_entity, añádelas al Map del
                        *RuleEntityUsageParticipant de su vertical (o crea uno extendiendo
                        EmployeeOwnedRuleEntityUsageParticipant). Si no, añádelas a EXEMPT en este
                        test con el motivo escrito.
                        """, undeclared())
                .isEmpty();
    }

    // La prueba de la guardia: una columna de código nueva aparece sola en el fallo. El DDL va
    // dentro de la transacción del test y se deshace con ella.
    @Test
    void aNewCodeColumnShowsUpByItself() {
        jdbcTemplate.execute("alter table employee.contact add column zz_probe_code varchar(5)");

        assertThat(undeclared()).containsExactly("contact.zz_probe_code");
    }

    // Una exención de una columna que ya no existe, o que además está declarada, es ruido que
    // acaba tapando un hueco real.
    @Test
    void exemptionsAndDeclarationsDoNotOverlapAndPointToRealColumns() {
        Set<String> schemaColumns = schemaColumns();
        Set<String> declared = declared();

        Map<String, String> stale = new TreeMap<>();
        EXEMPT.forEach((column, reason) -> {
            if (!schemaColumns.contains(column)) {
                stale.put(column, "ya no existe en el esquema");
            } else if (declared.contains(column)) {
                stale.put(column, "está declarada en un participante: sobra la exención");
            }
            if (reason == null || reason.isBlank()) {
                stale.put(column, "exención sin motivo");
            }
        });
        Set<String> declaredButMissing = new TreeSet<>(declared);
        declaredButMissing.removeAll(schemaColumns);

        assertThat(stale).as("exenciones caducadas").isEmpty();
        assertThat(declaredButMissing).as("columnas declaradas que el esquema no tiene").isEmpty();
    }

    private Set<String> undeclared() {
        Set<String> undeclared = schemaColumns();
        undeclared.removeAll(declared());
        undeclared.removeAll(EXEMPT.keySet());
        return undeclared;
    }

    private Set<String> schemaColumns() {
        return new TreeSet<>(jdbcTemplate.queryForList("""
                select table_name || '.' || column_name
                  from information_schema.columns
                 where table_schema = 'employee'
                   and column_name like '%\\_code'
                """, String.class));
    }

    private Set<String> declared() {
        return participants.stream()
                .filter(EmployeeOwnedRuleEntityUsageParticipant.class::isInstance)
                .map(EmployeeOwnedRuleEntityUsageParticipant.class::cast)
                .flatMap(participant -> participant.declaredColumns().stream())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
