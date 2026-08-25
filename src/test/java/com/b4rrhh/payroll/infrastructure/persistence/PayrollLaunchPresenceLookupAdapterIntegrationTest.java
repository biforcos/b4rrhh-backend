package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchEmployeeContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestSobreEsquemaReal
class PayrollLaunchPresenceLookupAdapterIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsPresenceFullyInsidePayrollMonth() {
        long employeeId = insertEmployee("EMP001");
        insertPresence(employeeId, 1, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20));

        assertPresenceNumbers("EMP001", 1);
    }

    @Test
    void findsPresenceStartingBeforeMonthAndEndingInsideMonth() {
        long employeeId = insertEmployee("EMP002");
        insertPresence(employeeId, 2, LocalDate.of(2024, 12, 20), LocalDate.of(2025, 1, 5));

        assertPresenceNumbers("EMP002", 2);
    }

    @Test
    void findsPresenceStartingInsideMonthAndEndingAfterMonth() {
        long employeeId = insertEmployee("EMP003");
        insertPresence(employeeId, 3, LocalDate.of(2025, 1, 25), LocalDate.of(2025, 2, 10));

        assertPresenceNumbers("EMP003", 3);
    }

    @Test
    void findsOpenEndedPresenceOverlappingMonth() {
        long employeeId = insertEmployee("EMP004");
        insertPresence(employeeId, 4, LocalDate.of(2025, 1, 1), null);

        assertPresenceNumbers("EMP004", 4);
    }

    @Test
    void ignoresPresenceEntirelyOutsidePayrollMonth() {
        long employeeId = insertEmployee("EMP005");
        insertPresence(employeeId, 5, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 10));

        assertPresenceNumbers("EMP005");
    }

    @Test
    void returnsMultipleCandidateUnitsWhenMultiplePresencesOverlapSameMonth() {
        long employeeId = insertEmployee("EMP006");
        insertPresence(employeeId, 6, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10));
        insertPresence(employeeId, 7, LocalDate.of(2025, 1, 15), null);

        assertPresenceNumbers("EMP006", 6, 7);
    }

    @Test
    void findsDistinctEmployeesWithPresenceInPeriodForBulkPopulation() {
        long employeeAId = insertEmployee("EMP007");
        long employeeBId = insertEmployee("EMP008");
        long employeeOutsideId = insertEmployee("EMP009");

        insertPresence(employeeAId, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10));
        insertPresence(employeeAId, 2, LocalDate.of(2025, 1, 15), null);
        insertPresence(employeeBId, 1, LocalDate.of(2024, 12, 25), LocalDate.of(2025, 1, 5));
        insertPresence(employeeOutsideId, 1, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 10));

        PayrollLaunchPresenceLookupAdapter adapter = new PayrollLaunchPresenceLookupAdapter(entityManager);
        List<PayrollLaunchEmployeeContext> results = adapter.findEmployeesWithPresenceInPeriod(
                "ESP",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31)
        );

        assertEquals(List.of("INTERNAL|EMP007", "INTERNAL|EMP008"),
                results.stream().map(item -> item.employeeTypeCode() + "|" + item.employeeNumber()).toList());
    }

    private void assertPresenceNumbers(String employeeNumber, Integer... expectedPresenceNumbers) {
        PayrollLaunchPresenceLookupAdapter adapter = new PayrollLaunchPresenceLookupAdapter(entityManager);
        List<PayrollLaunchPresenceContext> results = adapter.findRelevantPresences(
                "ESP",
                "INTERNAL",
                employeeNumber,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31)
        );

        assertEquals(List.of(expectedPresenceNumbers), results.stream().map(PayrollLaunchPresenceContext::presenceNumber).toList());
    }

    private long insertEmployee(String employeeNumber) {
        Integer existingRuleSystem = jdbcTemplate.queryForObject(
            "select count(*) from rulesystem.rule_system where code = ?",
            Integer.class,
            "ESP"
        );
        if (existingRuleSystem == null || existingRuleSystem == 0) {
            jdbcTemplate.update(
                "insert into rulesystem.rule_system (code, name, country_code, active) values (?, ?, ?, ?)",
                "ESP",
                "Spain",
                "ESP",
                true
            );
        }

        jdbcTemplate.update(
                "insert into employee.employee (rule_system_code, employee_type_code, employee_number, first_name, last_name_1, status) values (?, ?, ?, ?, ?, ?)",
                "ESP",
                "INTERNAL",
                employeeNumber,
                "Name",
                "Surname",
                "ACTIVE"
        );

        return jdbcTemplate.queryForObject("select max(id) from employee.employee", Long.class);
    }

    private void insertPresence(long employeeId, int presenceNumber, LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update(
                "insert into employee.presence (employee_id, presence_number, company_code, entry_reason_code, exit_reason_code, start_date, end_date) values (?, ?, ?, ?, ?, ?, ?)",
                employeeId,
                presenceNumber,
                "ES01",
                "HIRE",
                null,
                Date.valueOf(startDate),
                endDate == null ? null : Date.valueOf(endDate)
        );
    }
}