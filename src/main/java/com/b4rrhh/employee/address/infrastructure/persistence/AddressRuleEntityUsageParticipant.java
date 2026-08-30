package com.b4rrhh.employee.address.infrastructure.persistence;

import com.b4rrhh.employee.address.application.usecase.AddressRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.address} y en qué columna (backend#28). */
@Component
public class AddressRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public AddressRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "addresses",
                "address",
                Map.of(
                        AddressRuleEntityTypeCodes.EMPLOYEE_ADDRESS_TYPE, "address_type_code",
                        AddressRuleEntityTypeCodes.COUNTRY, "country_code"
                )
        );
    }
}
