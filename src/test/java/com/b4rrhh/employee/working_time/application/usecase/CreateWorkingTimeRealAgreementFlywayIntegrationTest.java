package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.support.TestSobreEsquemaReal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestSobreEsquemaReal
class CreateWorkingTimeRealAgreementFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String REAL_AGREEMENT_CODE = "99002405011982";
    private static final String REAL_AGREEMENT_CATEGORY_CODE = "99002405-G2";

    @Autowired
    private CreateWorkingTimeService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createPersistsWorkingTimeUsingAnnualHoursFromRealAgreementProfile() {
        String employeeNumber = "WT" + (System.nanoTime() % 1_000_000_000L);
        LocalDate startDate = LocalDate.of(2026, 1, 10);

        long employeeId = insertEmployee(employeeNumber);
        insertPresence(employeeId, startDate);
        insertLaborClassification(employeeId);

        var created = service.create(new CreateWorkingTimeCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber,
                startDate,
                null,
                new BigDecimal("50")
        ));

        BigDecimal persistedWeeklyHours = jdbcTemplate.queryForObject(
                "select weekly_hours from employee.working_time where employee_id = ? and working_time_number = 1",
                BigDecimal.class,
                employeeId
        );
        BigDecimal persistedDailyHours = jdbcTemplate.queryForObject(
                "select daily_hours from employee.working_time where employee_id = ? and working_time_number = 1",
                BigDecimal.class,
                employeeId
        );
        BigDecimal persistedMonthlyHours = jdbcTemplate.queryForObject(
                "select monthly_hours from employee.working_time where employee_id = ? and working_time_number = 1",
                BigDecimal.class,
                employeeId
        );

        assertNotNull(created.getId());
        assertEquals(1, created.getWorkingTimeNumber());
        assertEquals(0, new BigDecimal("16.69").compareTo(created.getWeeklyHours()));
        assertEquals(0, new BigDecimal("3.34").compareTo(created.getDailyHours()));
        assertEquals(0, new BigDecimal("72.33").compareTo(created.getMonthlyHours()));
        assertEquals(0, new BigDecimal("16.69").compareTo(persistedWeeklyHours));
        assertEquals(0, new BigDecimal("3.34").compareTo(persistedDailyHours));
        assertEquals(0, new BigDecimal("72.33").compareTo(persistedMonthlyHours));
        assertNotEquals(0, new BigDecimal("20.00").compareTo(created.getWeeklyHours()));
    }

    private long insertEmployee(String employeeNumber) {
        jdbcTemplate.update(
                """
                insert into employee.employee (
                    rule_system_code,
                    employee_type_code,
                    employee_number,
                    first_name,
                    last_name_1,
                    status,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber,
                "Real",
                "Agreement",
                "ACTIVE"
        );

        return jdbcTemplate.queryForObject(
                "select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                Long.class,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber
        );
    }

    private void insertPresence(long employeeId, LocalDate startDate) {
        jdbcTemplate.update(
                """
                insert into employee.presence (
                    employee_id,
                    presence_number,
                    company_code,
                    entry_reason_code,
                    start_date,
                    end_date,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                employeeId,
                1,
                "ES01",
                "HIRE",
                startDate,
                null
        );
    }

    private void insertLaborClassification(long employeeId) {
        jdbcTemplate.update(
                """
                insert into employee.labor_classification (
                    employee_id,
                    agreement_code,
                    agreement_category_code,
                    start_date,
                    end_date,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                employeeId,
                REAL_AGREEMENT_CODE,
                REAL_AGREEMENT_CATEGORY_CODE,
                LocalDate.of(2024, 1, 1),
                null
        );
    }
}