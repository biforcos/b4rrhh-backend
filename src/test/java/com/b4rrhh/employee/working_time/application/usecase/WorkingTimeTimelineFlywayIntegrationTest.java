package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five cases backend#49 is done when they hold, against the real schema:
 * the working time series is written through the temporal component and the
 * invariants of ADR-057 decide.
 */
@TestSobreEsquemaReal
class WorkingTimeTimelineFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String REAL_AGREEMENT_CODE = "99002405011982";
    private static final String REAL_AGREEMENT_CATEGORY_CODE = "99002405-G2";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY_16 = LocalDate.of(2026, 1, 16);
    private static final BigDecimal FULL_TIME = new BigDecimal("100");
    private static final BigDecimal HALF_TIME = new BigDecimal("50");

    @Autowired
    private CreateWorkingTimeService createService;
    @Autowired
    private UpdateWorkingTimeService updateService;
    @Autowired
    private DeleteWorkingTimeService deleteService;
    @Autowired
    private PlanWorkingTimeChangeService planService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private String employeeNumber;
    private Long employeeId;

    @BeforeEach
    void anEmployeePresentFromDayOne() {
        employeeNumber = "WT" + (System.nanoTime() % 1_000_000_000L);
        employeeId = DatosDePrueba.empleado(jdbcTemplate, employeeNumber);
        DatosDePrueba.presencia(jdbcTemplate, employeeId, 1, DAY_1, null);
        insertLaborClassification(employeeId);
    }

    @Test
    void addingFromTheSixteenthClosesTheOpenOneOnTheFifteenthInsteadOfReturningAConflict() {
        createService.create(create(DAY_1, null, FULL_TIME));

        WorkingTime second = createService.create(create(DAY_16, null, HALF_TIME));
        entityManager.flush();

        assertEquals(2, second.getWorkingTimeNumber());
        assertEquals(DAY_16, second.getStartDate());
        assertNull(second.getEndDate());
        assertEquals(DAY_15, persistedEndDate(1));
        assertNull(persistedEndDate(2));
    }

    @Test
    void addingOneThatLeavesAGapIsRejectedSayingWhichGap() {
        createService.create(create(DAY_1, null, FULL_TIME));

        WorkingTimeCoverageGapException ex = assertThrows(
                WorkingTimeCoverageGapException.class,
                () -> createService.create(create(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), HALF_TIME))
        );
        entityManager.flush();

        assertEquals(List.of(new WorkingTimePeriod(LocalDate.of(2026, 3, 1), null)), ex.gaps());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        createService.create(create(DAY_1, null, FULL_TIME));
        createService.create(create(DAY_16, null, HALF_TIME));

        deleteService.delete(new DeleteWorkingTimeCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2));
        entityManager.flush();

        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    @Test
    void deletingOneInTheMiddleIsRejectedSayingWhichNeighbourToStretch() {
        createService.create(create(DAY_1, null, FULL_TIME));
        createService.create(create(DAY_16, null, HALF_TIME));
        createService.create(create(LocalDate.of(2026, 2, 1), null, FULL_TIME));

        WorkingTimeCoverageGapException ex = assertThrows(
                WorkingTimeCoverageGapException.class,
                () -> deleteService.delete(new DeleteWorkingTimeCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2))
        );
        entityManager.flush();

        assertEquals(List.of(new WorkingTimePeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, DAY_1, DAY_15),
                        new WorkingTimeOccurrence(3, LocalDate.of(2026, 2, 1), null)
                ),
                ex.stretchCandidates()
        );
        assertEquals(3, persistedCount());
    }

    @Test
    void correctingTheDatesIsJudgedByTheSameInvariants() {
        createService.create(create(DAY_1, null, FULL_TIME));
        createService.create(create(DAY_16, null, HALF_TIME));

        WorkingTimeCoverageGapException ex = assertThrows(
                WorkingTimeCoverageGapException.class,
                () -> updateService.update(new UpdateWorkingTimeCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2, LocalDate.of(2026, 2, 1), null, HALF_TIME
                ))
        );
        entityManager.flush();

        assertEquals(List.of(new WorkingTimePeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertTrue(ex.stretchCandidates().contains(new WorkingTimeOccurrence(1, DAY_1, DAY_15)));
        assertEquals(DAY_16, persistedStartDate(2));
    }

    @Test
    void thePlanCanBeAskedForWithoutApplyingIt() {
        createService.create(create(DAY_1, null, FULL_TIME));

        WorkingTimePlan plan = planService.plan(new PlanWorkingTimeChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_16, null
        ));
        entityManager.flush();

        assertTrue(plan.isAccepted());
        assertEquals(1, plan.adjustedOccurrence().workingTimeNumber());
        assertEquals(DAY_15, plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, DAY_1, DAY_15),
                        new WorkingTimeOccurrence(null, DAY_16, null)
                ),
                plan.projected()
        );
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    private CreateWorkingTimeCommand create(LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
        return new CreateWorkingTimeCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, startDate, endDate, percentage);
    }

    private int persistedCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from employee.working_time where employee_id = ?",
                Integer.class,
                employeeId
        );
    }

    private LocalDate persistedEndDate(int workingTimeNumber) {
        return jdbcTemplate.queryForObject(
                "select end_date from employee.working_time where employee_id = ? and working_time_number = ?",
                LocalDate.class,
                employeeId,
                workingTimeNumber
        );
    }

    private LocalDate persistedStartDate(int workingTimeNumber) {
        return jdbcTemplate.queryForObject(
                "select start_date from employee.working_time where employee_id = ? and working_time_number = ?",
                LocalDate.class,
                employeeId,
                workingTimeNumber
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
