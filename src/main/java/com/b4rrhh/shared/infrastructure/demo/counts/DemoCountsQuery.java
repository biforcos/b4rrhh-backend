package com.b4rrhh.shared.infrastructure.demo.counts;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cuenta las filas que reset-demo.sh cuenta, en las mismas tablas.
 *
 * Va por SQL directo y no por los repositorios de cada contexto porque no es
 * una lectura de dominio: es la misma consulta que hace el script de reseteo
 * (count(*) sobre employee.employee, payroll.payroll y
 * payroll_engine.payroll_concept), y conviene que se vea que es la misma.
 * DemoCountsQueryIntegrationTest la pasa sobre el esquema real: si alguien
 * renombra una tabla, falla ahi y no en la portada.
 */
@Component
@Profile("demo")
@ConditionalOnProperty(prefix = "app.demo-auth", name = "enabled", havingValue = "true")
public class DemoCountsQuery {

    private final JdbcTemplate jdbcTemplate;

    public DemoCountsQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DemoCounts count() {
        return new DemoCounts(
                count("employee.employee"),
                count("payroll.payroll"),
                count("payroll_engine.payroll_concept"));
    }

    /** El nombre de la tabla es una constante de esta clase, nunca viene de fuera. */
    private long count(String table) {
        Long total = jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
        return total == null ? 0 : total;
    }
}
