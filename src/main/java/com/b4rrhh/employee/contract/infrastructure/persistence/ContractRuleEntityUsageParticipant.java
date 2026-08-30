package com.b4rrhh.employee.contract.infrastructure.persistence;

import com.b4rrhh.employee.contract.application.usecase.ContractRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.contract} y en qué columna (backend#28). */
@Component
public class ContractRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public ContractRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "contracts",
                "contract",
                Map.of(
                        ContractRuleEntityTypeCodes.CONTRACT, "contract_code",
                        ContractRuleEntityTypeCodes.CONTRACT_SUBTYPE, "contract_subtype_code"
                )
        );
    }
}
