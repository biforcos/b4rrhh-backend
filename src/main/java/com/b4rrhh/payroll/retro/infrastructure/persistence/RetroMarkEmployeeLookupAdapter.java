package com.b4rrhh.payroll.retro.infrastructure.persistence;

import com.b4rrhh.payroll.retro.application.port.RetroMarkEmployeeLookupPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RetroMarkEmployeeLookupAdapter implements RetroMarkEmployeeLookupPort {

    private final JdbcTemplate jdbcTemplate;

    public RetroMarkEmployeeLookupAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean exists(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        Boolean found = jdbcTemplate.queryForObject("""
                select exists (select 1 from employee.employee
                                where rule_system_code = ? and employee_type_code = ? and employee_number = ?)
                """, Boolean.class, ruleSystemCode, employeeTypeCode, employeeNumber);
        return Boolean.TRUE.equals(found);
    }
}
