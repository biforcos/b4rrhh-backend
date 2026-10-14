package com.b4rrhh.employee.presence.infrastructure.persistence;

import com.b4rrhh.employee.presence.application.port.PresencePayrollLookupPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lee los recibos cerrados de una presencia en {@code payroll.payroll}, que guarda la presencia de cada
 * recibo (backend#172). Sólo lectura.
 */
@Component
public class PresencePayrollJdbcAdapter implements PresencePayrollLookupPort {

    private final JdbcTemplate jdbcTemplate;

    public PresencePayrollJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> definitivePeriods(String ruleSystemCode, String employeeTypeCode, String employeeNumber,
                                          Integer presenceNumber) {
        return jdbcTemplate.queryForList("""
                select distinct payroll_period_code
                  from payroll.payroll
                 where rule_system_code = ? and employee_type_code = ? and employee_number = ?
                   and presence_number = ? and status = 'DEFINITIVE'
                 order by payroll_period_code
                """, String.class, ruleSystemCode, employeeTypeCode, employeeNumber, presenceNumber);
    }
}
