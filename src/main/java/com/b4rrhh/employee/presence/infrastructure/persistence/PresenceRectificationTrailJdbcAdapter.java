package com.b4rrhh.employee.presence.infrastructure.persistence;

import com.b4rrhh.employee.presence.application.port.PresenceRectificationTrailPort;
import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Escribe el rastro en {@code employee.presence_rectification} (V179, backend#172). */
@Component
public class PresenceRectificationTrailJdbcAdapter implements PresenceRectificationTrailPort {

    private final JdbcTemplate jdbcTemplate;

    public PresenceRectificationTrailJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(PresenceRectification rectification, String rectifiedBy, String moved) {
        jdbcTemplate.update("""
                insert into employee.presence_rectification
                       (employee_id, presence_number, field, old_date, new_date, rectified_by, moved)
                values (?, ?, ?, ?, ?, ?, ?)
                """, rectification.employeeId(), rectification.presenceNumber(), rectification.field().name(),
                rectification.oldDate(), rectification.newDate(), rectifiedBy, moved);
    }
}
