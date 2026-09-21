package com.b4rrhh.employee.extra_payment_regime.infrastructure.persistence;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimePresenceConsistencyPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Component
public class ExtraPaymentRegimePresenceConsistencyAdapter implements ExtraPaymentRegimePresenceConsistencyPort {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final EntityManager entityManager;

    public ExtraPaymentRegimePresenceConsistencyAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsPresenceContainingPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEndDate = endDate == null ? MAX_DATE : endDate;

        Object result = entityManager.createNativeQuery("""
                                select case when count(*) > 0 then true else false end
                from employee.presence p
                where p.employee_id = :employeeId
                  and p.start_date <= :startDate
                  and :effectiveEndDate <= coalesce(p.end_date, :maxDate)
                """)
                .setParameter("employeeId", employeeId)
                .setParameter("startDate", startDate)
                .setParameter("effectiveEndDate", effectiveEndDate)
                .setParameter("maxDate", MAX_DATE)
                .getSingleResult();

        if (result instanceof Boolean boolResult) {
            return boolResult;
        }
        if (result instanceof Number numberResult) {
            return numberResult.intValue() > 0;
        }

        return Boolean.parseBoolean(String.valueOf(result));
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
            throw new IllegalStateException("Unexpected row shape for extra payment regime presence query");
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

        throw new IllegalStateException(
                "Unexpected date type in extra payment regime presence query: " + value.getClass()
        );
    }
}