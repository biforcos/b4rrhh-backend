package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.CatalogColumnIntegrity;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

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
 * <b>El cálculo ya no vive en este test</b>, sino en {@link CheckCatalogCodeIntegrityUseCase}
 * (backend#44). El motivo es el mismo de siempre: la comprobación tiene que poder apuntarse a una
 * base poblada —la demo— desde el despliegue, y dos implementaciones de la misma regla se separan.
 * Aquí quedan las sondas, que son lo que demuestra el mecanismo, y el test principal, que en el
 * pipeline se ejecuta sobre cero filas y por eso no puede fallar: eso lo dice
 * {@link #theSuiteRunsThisCheckOverAnEmptySchemaAndThatIsNotAPass()}, en voz alta, en vez de
 * dejarlo pasar por verde.
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
    private CheckCatalogCodeIntegrityUseCase checkCatalogCodeIntegrityUseCase;

    @Test
    void noRowInTheEmployeeSchemaPointsToACodeThatDoesNotExist() {
        CatalogCodeIntegrityReport report = checkCatalogCodeIntegrityUseCase.check();

        assertThat(report.columns()).as("los participantes declaran columnas").isNotEmpty();

        assertThat(report.orphanColumns())
                .withFailMessage("""
                        Hay filas del esquema employee que apuntan a códigos que no existen en
                        rulesystem.rule_entity (columna, tipo de catálogo, filas y reglamentación/código):
                        %s

                        Un código sólo existe dentro de su reglamentación. Si el código es correcto, lo
                        que falta es darlo de alta en rule_entity (con la vigencia que le toque: aquí no
                        se juzga). Si es un residuo —de un fixture, de un script, de una migración—, hay
                        que corregir la fila. Los que nunca se limpian son los que acaban leyéndose mal
                        para siempre en el histórico.
                        """, report.describeOrphans())
                .isEmpty();
    }

    /**
     * Lo que el ADR-055 decía de más y el {@code backend#44} vino a corregir: en la suite esta
     * comprobación <b>no ve ninguna fila</b>. Ninguna de las migraciones inserta nada en
     * {@code employee}, y los fixtures de otras clases se deshacen con su transacción.
     *
     * <p>Así que el verde del test de arriba no dice que los datos estén bien: dice que no hay
     * datos. El valor aquí está entero en las tres sondas de abajo, que demuestran el mecanismo.
     * Comprobar el dato de verdad es lo que hace el perfil {@code comprobar-catalogos} contra una
     * base poblada, y este test lo deja escrito para que nadie confunda una cosa con la otra.
     */
    @Test
    void theSuiteRunsThisCheckOverAnEmptySchemaAndThatIsNotAPass() {
        CatalogCodeIntegrityReport report = checkCatalogCodeIntegrityUseCase.check();

        assertThat(report.totalRows())
                .withFailMessage("""
                        Esta comprobación ya ve filas en la suite (%d), y hasta hoy no veía ninguna.
                        Si es a propósito —han entrado semillas en employee— hay que reescribir este
                        test y la nota del ADR-055 que dice lo contrario. Si no es a propósito, hay
                        un fixture escapándose de su transacción.
                        Informe: %s
                        """, report.totalRows(), report.summary())
                .isZero();
        assertThat(report.emptyColumns()).hasSameSizeAs(report.columns());
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

        CatalogCodeIntegrityReport report = checkCatalogCodeIntegrityUseCase.check();

        assertThat(report.orphanColumns()).singleElement().satisfies(found -> {
            assertThat(found.qualifiedColumn()).isEqualTo("presence.entry_reason_code");
            assertThat(found.ruleEntityTypeCode()).isEqualTo("EMPLOYEE_PRESENCE_ENTRY_REASON");
            assertThat(found.orphanRows()).isEqualTo(1);
            assertThat(found.orphanCodes()).containsExactly(entry("ESP/ZZ_PROBE", 1L));
        });
        // Lo que se lee en el fallo: columna, tipo, cuántas de cuántas, y cuáles. Es lo que
        // convierte el fallo en una decisión: no es lo mismo un residuo de fixture que doscientos
        // convenios. El «de 1» es el denominador que el backend#44 añadió.
        assertThat(report.describeOrphans()).isEqualTo(
                "  presence.entry_reason_code (EMPLOYEE_PRESENCE_ENTRY_REASON): 1 de 1 fila(s): ESP/ZZ_PROBE (1)");
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
            assertThat(found.qualifiedColumn()).isEqualTo("contact.contact_type_code");
            assertThat(found.orphanCodes()).containsExactly(entry("ESP/ZZ_PROBE", 1L));
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
            assertThat(found.qualifiedColumn()).isEqualTo("employee_payroll_input.employee_type_code");
            assertThat(found.orphanCodes()).containsExactly(entry("ESP/ZZ_PROBE", 1L));
        });
    }

    private List<CatalogColumnIntegrity> orphans() {
        return checkCatalogCodeIntegrityUseCase.check().orphanColumns();
    }
}
