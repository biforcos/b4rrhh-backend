package com.b4rrhh.employee.cost_center.infrastructure.persistence;

import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Component
public class CostCenterPresenceConsistencyAdapter implements CostCenterPresenceConsistencyPort {

    private final EntityManager entityManager;

    public CostCenterPresenceConsistencyAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<PresencePeriod> findPresencePeriodsByEmployeeIdOrderByStartDate(Long employeeId) {
        List<?> rows = entityManager.createNativeQuery("""
                select start_date, end_date
                from employee.presence
                where employee_id = :employeeId
                order by start_date
                """)
                .setParameter("employeeId", employeeId)
                .getResultList();

        return rows.stream()
                .map(this::toPresencePeriod)
                .toList();
    }

    private PresencePeriod toPresencePeriod(Object row) {
        if (!(row instanceof Object[] columns) || columns.length < 2) {
            throw new IllegalStateException("Unexpected row shape for cost center presence consistency query");
        }

        return new PresencePeriod(toLocalDate(columns[0]), toLocalDate(columns[1]));
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        throw new IllegalStateException("Unexpected date type in cost center presence consistency query: " + value.getClass());
    }
}
