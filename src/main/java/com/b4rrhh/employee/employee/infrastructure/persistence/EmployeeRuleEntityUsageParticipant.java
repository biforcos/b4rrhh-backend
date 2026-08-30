package com.b4rrhh.employee.employee.infrastructure.persistence;

import com.b4rrhh.employee.employee.application.usecase.EmployeeRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant.RuleSystemSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.employee} y en qué columna (backend#29). El tipo es parte de la clave de negocio de todos los empleados: borrarlo los dejaría a todos apuntando a un tipo que no existe. */
@Component
public class EmployeeRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public EmployeeRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "employees",
                "employee",
                RuleSystemSource.OWN_COLUMN,
                Map.of(
                        EmployeeRuleEntityTypeCodes.EMPLOYEE_TYPE, "employee_type_code"
                )
        );
    }
}
