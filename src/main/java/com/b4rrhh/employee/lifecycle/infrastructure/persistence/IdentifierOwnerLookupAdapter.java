package com.b4rrhh.employee.lifecycle.infrastructure.persistence;

import com.b4rrhh.employee.lifecycle.application.model.IdentifierOwner;
import com.b4rrhh.employee.lifecycle.application.port.IdentifierOwnerLookupPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.Optional;

@Component
public class IdentifierOwnerLookupAdapter implements IdentifierOwnerLookupPort {

    private final JdbcTemplate jdbcTemplate;

    public IdentifierOwnerLookupAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // De alta es tener hoy una presencia abierta o que acaba en el futuro: un cese con fecha
    // futura todavia no ha ocurrido.
    @Override
    public Optional<IdentifierOwner> findOwner(String ruleSystemCode, String identifierTypeCode, String identifierValue) {
        if (identifierValue == null || identifierValue.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query("""
                select e.employee_type_code,
                       e.employee_number,
                       exists (select 1 from employee.presence p
                                where p.employee_id = e.id
                                  and (p.end_date is null or p.end_date >= current_date)) as active,
                       (select max(p.end_date) from employee.presence p
                         where p.employee_id = e.id) as ceased_on
                  from employee.identifier i
                  join employee.employee e on e.id = i.employee_id
                 where e.rule_system_code = ?
                   and i.identifier_type_code = ?
                   and upper(trim(i.identifier_value)) = upper(trim(?))
                 order by e.employee_number
                 limit 1
                """,
                (rs, rowNum) -> {
                    Date ceasedOn = rs.getDate("ceased_on");
                    boolean active = rs.getBoolean("active");
                    return new IdentifierOwner(
                            rs.getString("employee_type_code"),
                            rs.getString("employee_number"),
                            active,
                            active || ceasedOn == null ? null : ceasedOn.toLocalDate()
                    );
                },
                ruleSystemCode, identifierTypeCode, identifierValue
        ).stream().findFirst();
    }
}
