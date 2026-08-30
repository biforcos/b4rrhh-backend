package com.b4rrhh.employee.cost_center.infrastructure.persistence;

import com.b4rrhh.employee.cost_center.application.usecase.CostCenterRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.cost_center} y en qué columna (backend#28). */
@Component
public class CostCenterRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public CostCenterRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "cost-centers",
                "cost_center",
                Map.of(
                        CostCenterRuleEntityTypeCodes.COST_CENTER, "cost_center_code"
                )
        );
    }
}
