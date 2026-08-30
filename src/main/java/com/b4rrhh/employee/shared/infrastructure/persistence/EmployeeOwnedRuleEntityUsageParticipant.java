package com.b4rrhh.employee.shared.infrastructure.persistence;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base de los participantes de uso de catálogo de las tablas del esquema {@code employee}: el
 * vertical declara su tabla y qué columna guarda cada tipo de código, y esto cuenta. Se
 * cuentan todas las filas, vigentes o no: el histórico se leería mal para siempre si el
 * código desapareciera (backend#28).
 *
 * La reglamentación normalmente no está en la fila del vertical sino en el empleado, de ahí
 * el join; las tablas que llevan su propio {@code rule_system_code} —{@code employee} y
 * {@code employee_payroll_input}— lo dicen con {@link RuleSystemSource#OWN_COLUMN}. Tabla y
 * columnas vienen de constantes del vertical, nunca de la petición.
 */
public abstract class EmployeeOwnedRuleEntityUsageParticipant implements RuleEntityUsageParticipant {

    /** Dónde está el {@code rule_system_code} que acota el recuento. */
    public enum RuleSystemSource {
        /** En {@code employee.employee}, a través de {@code employee_id}. */
        OWNER_EMPLOYEE,
        /** En la propia tabla. */
        OWN_COLUMN
    }

    private final JdbcTemplate jdbcTemplate;
    private final String resource;
    private final String table;
    private final RuleSystemSource ruleSystemSource;
    private final Map<String, String> columnByRuleEntityTypeCode;

    protected EmployeeOwnedRuleEntityUsageParticipant(
            JdbcTemplate jdbcTemplate,
            String resource,
            String table,
            Map<String, String> columnByRuleEntityTypeCode
    ) {
        this(jdbcTemplate, resource, table, RuleSystemSource.OWNER_EMPLOYEE, columnByRuleEntityTypeCode);
    }

    protected EmployeeOwnedRuleEntityUsageParticipant(
            JdbcTemplate jdbcTemplate,
            String resource,
            String table,
            RuleSystemSource ruleSystemSource,
            Map<String, String> columnByRuleEntityTypeCode
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.resource = resource;
        this.table = table;
        this.ruleSystemSource = ruleSystemSource;
        this.columnByRuleEntityTypeCode = Map.copyOf(columnByRuleEntityTypeCode);
    }

    @Override
    public final String resource() {
        return resource;
    }

    /** Las columnas que este participante cubre, como {@code tabla.columna}; para el guardarraíl del esquema. */
    public final Set<String> declaredColumns() {
        return columnByRuleEntityTypeCode.values().stream()
                .map(column -> table + "." + column)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public final long countReferences(String ruleSystemCode, String ruleEntityTypeCode, String code) {
        String column = columnByRuleEntityTypeCode.get(ruleEntityTypeCode);
        if (column == null) {
            return 0;
        }

        String sql = ruleSystemSource == RuleSystemSource.OWN_COLUMN
                ? "select count(*) from employee." + table + " owned"
                        + " where owned.rule_system_code = ? and owned." + column + " = ?"
                : "select count(*) from employee." + table + " owned"
                        + " join employee.employee e on e.id = owned.employee_id"
                        + " where e.rule_system_code = ? and owned." + column + " = ?";

        Long count = jdbcTemplate.queryForObject(sql, Long.class, ruleSystemCode, code);
        return count == null ? 0 : count;
    }
}
