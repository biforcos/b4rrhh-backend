package com.b4rrhh.employee.contact.infrastructure.persistence;

import com.b4rrhh.employee.contact.application.usecase.ContactRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.contact} y en qué columna (backend#28). */
@Component
public class ContactRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public ContactRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "contacts",
                "contact",
                Map.of(
                        ContactRuleEntityTypeCodes.CONTACT_TYPE, "contact_type_code"
                )
        );
    }
}
