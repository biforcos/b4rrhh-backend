package com.b4rrhh.employee.address.infrastructure.persistence;

import com.b4rrhh.employee.address.application.port.AddressPresenceLookupPort;
import com.b4rrhh.employee.temporal.support.DateRange;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Component
public class AddressPresenceLookupAdapter implements AddressPresenceLookupPort {

    private final EntityManager entityManager;

    public AddressPresenceLookupAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DateRange> findPresencePeriodsByEmployeeIdOrderByStartDate(Long employeeId) {
        List<?> rows = entityManager.createNativeQuery("""
                select start_date, end_date
                from employee.presence
                where employee_id = :employeeId
                order by start_date
                """)
                .setParameter("employeeId", employeeId)
                .getResultList();

        return rows.stream()
                .map(this::toDateRange)
                .toList();
    }

    private DateRange toDateRange(Object row) {
        if (!(row instanceof Object[] columns) || columns.length < 2) {
            throw new IllegalStateException("Unexpected row shape for address presence query");
        }

        return new DateRange(toLocalDate(columns[0]), toLocalDate(columns[1]));
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

        throw new IllegalStateException("Unexpected date type in address presence query: " + value.getClass());
    }
}
