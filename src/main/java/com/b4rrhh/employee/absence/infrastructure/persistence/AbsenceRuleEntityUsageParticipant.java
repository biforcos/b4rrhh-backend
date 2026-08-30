package com.b4rrhh.employee.absence.infrastructure.persistence;

import com.b4rrhh.employee.absence.application.usecase.AbsenceRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.employee_absence} y en qué columna (backend#29). */
@Component
public class AbsenceRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public AbsenceRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "absences",
                "employee_absence",
                Map.of(
                        AbsenceRuleEntityTypeCodes.EMPLOYEE_ABSENCE_TYPE, "absence_type_code"
                )
        );
    }
}
