package com.b4rrhh.employee.workcenter.infrastructure.persistence;

import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class WorkCenterPresenceConsistencyAdapter implements WorkCenterPresenceConsistencyPort {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final EntityManager entityManager;

    public WorkCenterPresenceConsistencyAdapter(EntityManager entityManager) {
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
    public Optional<String> findActiveCompanyCode(Long employeeId, LocalDate referenceDate) {
        List<?> rows = entityManager.createNativeQuery("""
                select p.company_code
                from employee.presence p
                where p.employee_id = :employeeId
                  and p.start_date <= :referenceDate
                  and :referenceDate <= coalesce(p.end_date, :maxDate)
                order by p.start_date desc, p.presence_number desc
                """)
                .setParameter("employeeId", employeeId)
                .setParameter("referenceDate", referenceDate)
                .setParameter("maxDate", MAX_DATE)
                .setMaxResults(1)
                .getResultList();

        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }

        String companyCode = String.valueOf(rows.get(0)).trim();
        if (companyCode.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(companyCode.toUpperCase());
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
            throw new IllegalStateException("Unexpected row shape for work center presence consistency query");
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

        throw new IllegalStateException("Unexpected date type in work center presence consistency query: " + value.getClass());
    }
}