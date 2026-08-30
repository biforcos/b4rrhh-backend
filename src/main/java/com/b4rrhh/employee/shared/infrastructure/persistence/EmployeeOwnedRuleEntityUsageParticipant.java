package com.b4rrhh.employee.shared.infrastructure.persistence;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * Base de los participantes de uso de catálogo de las tablas que cuelgan de
 * {@code employee.employee}: el vertical declara su tabla y qué columna guarda cada tipo de
 * código, y esto cuenta. Se cuentan todas las filas, vigentes o no: el histórico se leería
 * mal para siempre si el código desapareciera (backend#28).
 *
 * La reglamentación no está en la fila del vertical sino en el empleado, de ahí el join.
 * Tabla y columnas vienen de constantes del vertical, nunca de la petición.
 */
public abstract class EmployeeOwnedRuleEntityUsageParticipant implements RuleEntityUsageParticipant {

    private final JdbcTemplate jdbcTemplate;
    private final String resource;
    private final String table;
    private final Map<String, String> columnByRuleEntityTypeCode;

    protected EmployeeOwnedRuleEntityUsageParticipant(
            JdbcTemplate jdbcTemplate,
            String resource,
            String table,
            Map<String, String> columnByRuleEntityTypeCode
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.resource = resource;
        this.table = table;
        this.columnByRuleEntityTypeCode = Map.copyOf(columnByRuleEntityTypeCode);
    }

    @Override
    public final String resource() {
        return resource;
    }

    @Override
    public final long countReferences(String ruleSystemCode, String ruleEntityTypeCode, String code) {
        String column = columnByRuleEntityTypeCode.get(ruleEntityTypeCode);
        if (column == null) {
            return 0;
        }

        Long count = jdbcTemplate.queryForObject(
                "select count(*) from employee." + table + " owned"
                        + " join employee.employee e on e.id = owned.employee_id"
                        + " where e.rule_system_code = ? and owned." + column + " = ?",
                Long.class,
                ruleSystemCode,
                code
        );
        return count == null ? 0 : count;
    }
}
