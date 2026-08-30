package com.b4rrhh.employee.identifier.infrastructure.persistence;

import com.b4rrhh.employee.identifier.application.usecase.IdentifierRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.identifier} y en qué columna (backend#28). */
@Component
public class IdentifierRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public IdentifierRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "identifiers",
                "identifier",
                Map.of(
                        IdentifierRuleEntityTypeCodes.EMPLOYEE_IDENTIFIER_TYPE, "identifier_type_code",
                        IdentifierRuleEntityTypeCodes.COUNTRY, "issuing_country_code"
                )
        );
    }
}
