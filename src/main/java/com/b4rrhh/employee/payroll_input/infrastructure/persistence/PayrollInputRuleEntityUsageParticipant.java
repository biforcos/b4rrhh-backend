package com.b4rrhh.employee.payroll_input.infrastructure.persistence;

import com.b4rrhh.employee.payroll_input.application.usecase.PayrollInputRuleEntityTypeCodes;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant.RuleSystemSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Qué códigos de catálogo guarda {@code employee.employee_payroll_input} y en qué columna (backend#29). {@code concept_code} no va: es un concepto del motor de nómina, no una fila de {@code rule_entity}. */
@Component
public class PayrollInputRuleEntityUsageParticipant extends EmployeeOwnedRuleEntityUsageParticipant {

    public PayrollInputRuleEntityUsageParticipant(JdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "payroll-inputs",
                "employee_payroll_input",
                RuleSystemSource.OWN_COLUMN,
                Map.of(
                        PayrollInputRuleEntityTypeCodes.EMPLOYEE_TYPE, "employee_type_code"
                )
        );
    }
}
