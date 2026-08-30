package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.employee.labor_classification.application.usecase.LaborClassificationRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.labor_classification} y en qué columna (backend#28). */
@Component
public class LaborClassificationRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public LaborClassificationRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "labor-classifications",
                "labor_classification",
                Map.of(
                        LaborClassificationRuleEntityTypeCodes.AGREEMENT, "agreement_code",
                        LaborClassificationRuleEntityTypeCodes.AGREEMENT_CATEGORY, "agreement_category_code"
                )
        );
    }
}
