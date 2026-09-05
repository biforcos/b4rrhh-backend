package com.b4rrhh.shared.infrastructure.demo.counts;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Las tres cuentas sobre el esquema real (backend#45, criterio 4).
 *
 * Se mide la diferencia y no el total: las migraciones siembran conceptos de
 * nomina y otros tests del mismo contexto pueden dejar filas. Lo que importa
 * es que cada tabla es la que reset-demo.sh cuenta: si alguien la renombra,
 * la consulta revienta aqui y no en la portada.
 *
 * La consulta se instancia a mano y no se inyecta: es un bean solo con el
 * perfil 'demo', y este contexto compartido no lo lleva.
 */
@TestSobreEsquemaReal
class DemoCountsQueryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void countsEmployeesCalculatedPayrollsAndPayrollConcepts() {
        DemoCountsQuery query = new DemoCountsQuery(jdbcTemplate);
        DemoCounts before = query.count();

        DatosDePrueba.empleado(jdbcTemplate);
        insertCalculatedPayroll();
        insertPayrollConcept();

        DemoCounts after = query.count();
        assertEquals(before.employees() + 1, after.employees());
        assertEquals(before.calculatedPayrolls() + 1, after.calculatedPayrolls());
        assertEquals(before.payrollConcepts() + 1, after.payrollConcepts());
    }

    private void insertCalculatedPayroll() {
        Timestamp calculatedAt = Timestamp.valueOf(LocalDateTime.of(2026, 1, 31, 10, 15));
        jdbcTemplate.update(
                "insert into payroll.payroll (rule_system_code, employee_type_code, employee_number, "
                        + "payroll_period_code, payroll_type_code, presence_number, status, status_reason_code, "
                        + "calculated_at, calculation_engine_code, calculation_engine_version, created_at, updated_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "ESP", "INTERNAL", "DEMOCOUNTS", "202601", "NORMAL", 1, "CALCULATED", null,
                calculatedAt, "ENGINE", "1.0", calculatedAt, calculatedAt);
    }

    private void insertPayrollConcept() {
        jdbcTemplate.update("insert into payroll_engine.payroll_object "
                + "(rule_system_code, object_type_code, object_code, created_at, updated_at) "
                + "values ('ESP', 'CONCEPT', 'DEMO_COUNTS_TEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        Long objectId = jdbcTemplate.queryForObject(
                "select id from payroll_engine.payroll_object "
                        + "where rule_system_code = 'ESP' and object_type_code = 'CONCEPT' "
                        + "and object_code = 'DEMO_COUNTS_TEST'",
                Long.class);
        jdbcTemplate.update("insert into payroll_engine.payroll_concept "
                        + "(object_id, concept_mnemonic, calculation_type, functional_nature, "
                        + "payslip_order_code, execution_scope, created_at, updated_at) "
                        + "values (?, 'DEMO_COUNTS_TEST', 'DIRECT_AMOUNT', 'EARNING', "
                        + "'DEMO_COUNTS_TEST', 'PERIOD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                objectId);
    }
}
