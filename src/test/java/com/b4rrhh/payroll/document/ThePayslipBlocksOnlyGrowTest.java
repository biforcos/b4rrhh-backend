package com.b4rrhh.payroll.document;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Los bloques y apartados del recibo solo crecen ({@code backend#174}, punto 1, V181).
 *
 * <p>El documento de un recibo cerrado se vuelve a dibujar cuando falta, y lo unico que lee que no
 * va congelado en sus lineas es el nombre y el orden de su bloque y de su apartado. Si eso pudiera
 * cambiar, el papel regenerado diria otra cosa que el que se entrego. Aqui se comprueban las dos
 * mitades: cambiar lo que se imprime no se puede, y anadir si.
 *
 * <p>Un intento por test, y no es estilo: el test corre dentro de una transaccion, y despues del
 * primer error Postgres rechaza todo lo que venga con «current transaction is aborted». El segundo
 * intento fallaria igual, pero por otra razon, y el mensaje no la distinguiria.
 */
@TestSobreEsquemaReal
class ThePayslipBlocksOnlyGrowTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void renamingABlockIsRefused() {
        assertThatThrownBy(() -> jdbc.update(
                "update payroll_engine.payslip_section set section_label = 'Percepciones'"
                        + " where rule_system_code = 'ESP' and section_code = 'DEVENGOS'"))
                .hasMessageContaining("payslip_section solo crece")
                .hasMessageContaining("backend#174");
    }

    @Test
    void reorderingABlockIsRefused() {
        assertThatThrownBy(() -> jdbc.update(
                "update payroll_engine.payslip_section set display_order = 15"
                        + " where rule_system_code = 'ESP' and section_code = 'DEVENGOS'"))
                .hasMessageContaining("payslip_section solo crece");
    }

    @Test
    void deletingABlockIsRefused() {
        assertThatThrownBy(() -> jdbc.update(
                "delete from payroll_engine.payslip_section"
                        + " where rule_system_code = 'ESP' and section_code = 'APORTACION_EMPRESARIAL'"))
                .hasMessageContaining("payslip_section no admite borrar");
    }

    @Test
    void renamingASubsectionIsRefused() {
        assertThatThrownBy(() -> jdbc.update(
                "update payroll_engine.payslip_subsection set subsection_label = '1. Comunes'"
                        + " where rule_system_code = 'ESP' and subsection_code = 'BASE_CC'"))
                .hasMessageContaining("payslip_subsection solo crece");
    }

    @Test
    void deletingASubsectionIsRefused() {
        assertThatThrownBy(() -> jdbc.update(
                "delete from payroll_engine.payslip_subsection"
                        + " where rule_system_code = 'ESP' and subsection_code = 'BASE_HE'"))
                .hasMessageContaining("payslip_subsection no admite borrar");
    }

    /** Lo que no se imprime sigue libre, y anadir un bloque nuevo no toca ningun recibo. */
    @Test
    void addingABlockAndTouchingTheTimestampsStillWork() {
        jdbc.update("update payroll_engine.payslip_section set updated_at = current_timestamp"
                + " where rule_system_code = 'ESP' and section_code = 'DEVENGOS'");

        jdbc.update("insert into payroll_engine.payslip_section"
                + " (rule_system_code, section_code, section_label, display_order)"
                + " values ('ESP', 'OTRO_BLOQUE', 'Otro bloque', 60)");

        assertThat(jdbc.queryForObject(
                "select count(*) from payroll_engine.payslip_section where rule_system_code = 'ESP'",
                Integer.class)).isEqualTo(6);
    }
}
