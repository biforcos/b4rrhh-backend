package com.b4rrhh.employee.workcenter.infrastructure.persistence;

import com.b4rrhh.employee.workcenter.application.usecase.WorkCenterRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.work_center} y en qué columna (backend#28). */
@Component
public class WorkCenterRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public WorkCenterRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "work-centers",
                "work_center",
                Map.of(
                        WorkCenterRuleEntityTypeCodes.WORK_CENTER, "work_center_code"
                )
        );
    }
}
