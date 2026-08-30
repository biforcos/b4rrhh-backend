package com.b4rrhh.employee.presence.infrastructure.persistence;

import com.b4rrhh.employee.presence.application.usecase.PresenceRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.presence} y en qué columna (backend#28). */
@Component
public class PresenceRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public PresenceRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "presences",
                "presence",
                Map.of(
                        PresenceRuleEntityTypeCodes.COMPANY, "company_code",
                        PresenceRuleEntityTypeCodes.EMPLOYEE_PRESENCE_ENTRY_REASON, "entry_reason_code",
                        PresenceRuleEntityTypeCodes.EMPLOYEE_PRESENCE_EXIT_REASON, "exit_reason_code"
                )
        );
    }
}
