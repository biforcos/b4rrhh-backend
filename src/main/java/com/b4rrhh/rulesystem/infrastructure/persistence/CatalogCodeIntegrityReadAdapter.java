package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant.CatalogColumnUsage;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant.RuleSystemSource;
import com.b4rrhh.rulesystem.application.port.CatalogCodeIntegrityReadPort;
import com.b4rrhh.rulesystem.application.port.CatalogColumnIntegrity;
import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cruza cada columna de catálogo declarada contra {@code rulesystem.rule_entity} (backend#43,
 * backend#44).
 *
 * <p>Tabla, columna y tipo vienen del participante —constantes del vertical—, igual que en
 * {@code countReferences}; el único parámetro de la petición es el tipo de catálogo. Nada de lo
 * que se interpola en el SQL sale de fuera.
 *
 * <p>Dos reglas, y son las del ADR-055. <b>Un código sólo existe dentro de su reglamentación</b>,
 * propia en la tabla o heredada del empleado según declare el participante: una comprobación que
 * mirase sólo el código daría verde con datos rotos. Y <b>se miran todas las filas, vigentes o
 * no</b>, igual que {@code countReferences}: el histórico se leería mal para siempre si el código
 * desapareciera. La vigencia no se juzga aquí.
 *
 * <p>Se cuentan las filas de la columna aunque no haya ni una huérfana, porque el denominador es
 * la mitad del resultado: sin él, «cero huérfanos» sobre una base recién migrada y «cero
 * huérfanos» sobre mil empleados se confunden.
 */
@Component
public class CatalogCodeIntegrityReadAdapter implements CatalogCodeIntegrityReadPort {

    private final JdbcTemplate jdbcTemplate;
    private final List<RuleEntityUsageParticipant> participants;

    public CatalogCodeIntegrityReadAdapter(
            JdbcTemplate jdbcTemplate,
            List<RuleEntityUsageParticipant> participants
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.participants = participants;
    }

    @Override
    public List<CatalogColumnIntegrity> readAll() {
        return declaredUsages().stream()
                .map(this::read)
                .sorted(Comparator.comparing(CatalogColumnIntegrity::qualifiedColumn))
                .toList();
    }

    private CatalogColumnIntegrity read(CatalogColumnUsage usage) {
        String ruleSystemCode = switch (usage.ruleSystemSource()) {
            case OWN_COLUMN -> "owned.rule_system_code";
            case OWNER_EMPLOYEE -> "e.rule_system_code";
        };
        String join = usage.ruleSystemSource() == RuleSystemSource.OWNER_EMPLOYEE
                ? " join employee.employee e on e.id = owned.employee_id"
                : "";
        String from = " from employee." + usage.table() + " owned" + join
                + " where owned." + usage.column() + " is not null";
        String missing = " and not exists (select 1 from rulesystem.rule_entity re"
                + "                        where re.rule_system_code = " + ruleSystemCode
                + "                          and re.rule_entity_type_code = ?"
                + "                          and re.code = owned." + usage.column() + ")";

        Long rows = jdbcTemplate.queryForObject("select count(*)" + from, Long.class);

        Map<String, Long> orphanCodes = new LinkedHashMap<>();
        jdbcTemplate.query(
                "select " + ruleSystemCode + " as rule_system_code, owned." + usage.column() + " as code,"
                        + "       count(*) as n"
                        + from + missing
                        + " group by 1, 2 order by 3 desc, 1, 2",
                (RowCallbackHandler) row -> orphanCodes.put(
                        row.getString("rule_system_code") + "/" + row.getString("code"),
                        row.getLong("n")),
                usage.ruleEntityTypeCode());

        return new CatalogColumnIntegrity(
                usage.qualifiedColumn(),
                usage.ruleEntityTypeCode(),
                rows == null ? 0 : rows,
                orphanCodes.values().stream().mapToLong(Long::longValue).sum(),
                orphanCodes);
    }

    private Set<CatalogColumnUsage> declaredUsages() {
        return participants.stream()
                .filter(EmployeeOwnedRuleEntityUsageParticipant.class::isInstance)
                .map(EmployeeOwnedRuleEntityUsageParticipant.class::cast)
                .flatMap(participant -> participant.declaredUsages().stream())
                .collect(Collectors.toUnmodifiableSet());
    }
}
