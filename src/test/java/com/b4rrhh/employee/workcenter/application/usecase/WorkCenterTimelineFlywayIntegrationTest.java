package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterIsACorrectionException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five cases of backend#48, plus the correction asked for as an add
 * (backend#52) and the bounded delete (backend#50), against the real schema:
 * the work center series is written through the temporal component and the
 * invariants of ADR-057 decide. The catalog is the seeded one: MAIN_OFFICE
 * and BRANCH_NORTH belong to ES01 (V52), the company of the
 * presence {@link DatosDePrueba#presencia} creates.
 */
@TestSobreEsquemaReal
class WorkCenterTimelineFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String MAIN_OFFICE = "MAIN_OFFICE";
    private static final String BRANCH_NORTH = "BRANCH_NORTH";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY_16 = LocalDate.of(2026, 1, 16);
    private static final LocalDate FEB_1 = LocalDate.of(2026, 2, 1);

    @Autowired
    private CreateWorkCenterService createService;
    @Autowired
    private UpdateWorkCenterService updateService;
    @Autowired
    private DeleteWorkCenterService deleteService;
    @Autowired
    private PlanWorkCenterChangeService planService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private String employeeNumber;
    private Long employeeId;

    @BeforeEach
    void anEmployeePresentFromDayOne() {
        employeeNumber = "WC" + (System.nanoTime() % 1_000_000_000L);
        employeeId = DatosDePrueba.empleado(jdbcTemplate, employeeNumber);
        DatosDePrueba.presencia(jdbcTemplate, employeeId, 1, DAY_1, null);
    }

    @Test
    void addingFromTheSixteenthClosesTheOpenOneOnTheFifteenthInsteadOfReturningAConflict() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));

        WorkCenter second = createService.create(create(BRANCH_NORTH, DAY_16, null));
        entityManager.flush();

        assertEquals(2, second.getWorkCenterAssignmentNumber());
        assertEquals(DAY_16, second.getStartDate());
        assertNull(second.getEndDate());
        assertEquals(2, persistedCount());
        assertEquals(DAY_15, persistedEndDate(1));
        assertNull(persistedEndDate(2));
        assertEquals(MAIN_OFFICE, persistedCode(1));
        assertEquals(BRANCH_NORTH, persistedCode(2));
    }

    @Test
    void addingOneThatLeavesAGapIsRejectedSayingWhichGap() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> createService.create(create(BRANCH_NORTH, FEB_1, LocalDate.of(2026, 2, 28)))
        );
        entityManager.flush();

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 3, 1), null)), ex.gaps());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));
        createService.create(create(BRANCH_NORTH, DAY_16, null));

        deleteService.delete(new DeleteWorkCenterCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2));
        entityManager.flush();

        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    @Test
    void deletingOneInTheMiddleIsRejectedSayingWhichNeighbourToStretch() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));
        createService.create(create(BRANCH_NORTH, DAY_16, null));
        createService.create(create(MAIN_OFFICE, FEB_1, null));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> deleteService.delete(new DeleteWorkCenterCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2))
        );
        entityManager.flush();

        assertEquals(List.of(new WorkCenterPeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, DAY_1, DAY_15),
                        new WorkCenterOccurrence(3, FEB_1, null)
                ),
                ex.stretchCandidates()
        );
        assertEquals(3, persistedCount());
    }

    // The old rule ("the assignment that starts a presence cannot be deleted") survives as a
    // case of the gap invariant: the only assignment starts the presence and cannot go.
    @Test
    void deletingTheAssignmentThatStartsThePresenceIsRejectedAsAGap() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> deleteService.delete(new DeleteWorkCenterCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 1))
        );
        entityManager.flush();

        assertEquals(List.of(new WorkCenterPeriod(DAY_1, null)), ex.gaps());
        assertEquals(1, persistedCount());
    }

    @Test
    void correctingTheDatesIsJudgedByTheSameInvariants() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));
        createService.create(create(BRANCH_NORTH, DAY_16, null));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> updateService.update(new UpdateWorkCenterCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2, BRANCH_NORTH, FEB_1, null
                ))
        );
        entityManager.flush();

        assertEquals(List.of(new WorkCenterPeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertTrue(ex.stretchCandidates().contains(new WorkCenterOccurrence(1, DAY_1, DAY_15)));
        assertEquals(2, persistedCount());
        assertEquals(DAY_16, persistedStartDate(2));
    }

    @Test
    void thePlanCanBeAskedForWithoutApplyingIt() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));

        WorkCenterPlan plan = planService.plan(new PlanWorkCenterChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_16, null
        ));
        entityManager.flush();

        assertTrue(plan.isAccepted());
        assertEquals(1, plan.adjustedOccurrence().workCenterAssignmentNumber());
        assertEquals(DAY_15, plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(new WorkCenterOccurrence(1, DAY_1, DAY_15), new WorkCenterOccurrence(null, DAY_16, null)),
                plan.projected()
        );
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    @Test
    void addingOnTheStartDateOfTheExistingOneIsRejectedAsItsCorrectionAndPersistsNothing() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));

        WorkCenterIsACorrectionException ex = assertThrows(
                WorkCenterIsACorrectionException.class,
                () -> createService.create(create(BRANCH_NORTH, DAY_1, null))
        );
        entityManager.flush();

        assertEquals(new WorkCenterOccurrence(1, DAY_1, null), ex.correctedOccurrence());
        assertTrue(ex.getMessage().contains("correct"), ex.getMessage());
        assertEquals(1, persistedCount());
        assertEquals(MAIN_OFFICE, persistedCode(1));
    }

    @Test
    void thePlanSaysAnAddOnAnExistingStartDateIsACorrectionOfThatAssignment() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));

        WorkCenterPlan plan = planService.plan(new PlanWorkCenterChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_1, DAY_15
        ));
        entityManager.flush();

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new WorkCenterOccurrence(1, DAY_1, null), plan.correctedOccurrence());
        assertEquals(List.of(new WorkCenterOccurrence(1, DAY_1, DAY_15)), plan.projected());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    // The correction the plan proposes, asked for as such, is the PUT.
    @Test
    void theCorrectionAskedForAsSuchReplacesTheCodeWithoutADuplicateRow() {
        createService.create(create(MAIN_OFFICE, DAY_1, null));

        WorkCenter corrected = updateService.update(new UpdateWorkCenterCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 1, BRANCH_NORTH, DAY_1, null
        ));
        entityManager.flush();

        assertEquals(BRANCH_NORTH, corrected.getWorkCenterCode());
        assertEquals(1, corrected.getWorkCenterAssignmentNumber());
        assertEquals(1, persistedCount());
        assertEquals(BRANCH_NORTH, persistedCode(1));
    }

    private CreateWorkCenterCommand create(String workCenterCode, LocalDate startDate, LocalDate endDate) {
        return new CreateWorkCenterCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, workCenterCode, startDate, endDate);
    }

    private int persistedCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from employee.work_center where employee_id = ?",
                Integer.class,
                employeeId
        );
    }

    private LocalDate persistedStartDate(int assignmentNumber) {
        return jdbcTemplate.queryForObject(
                "select start_date from employee.work_center where employee_id = ? and work_center_assignment_number = ?",
                LocalDate.class,
                employeeId,
                assignmentNumber
        );
    }

    private LocalDate persistedEndDate(int assignmentNumber) {
        return jdbcTemplate.queryForObject(
                "select end_date from employee.work_center where employee_id = ? and work_center_assignment_number = ?",
                LocalDate.class,
                employeeId,
                assignmentNumber
        );
    }

    private String persistedCode(int assignmentNumber) {
        return jdbcTemplate.queryForObject(
                "select work_center_code from employee.work_center where employee_id = ? and work_center_assignment_number = ?",
                String.class,
                employeeId,
                assignmentNumber
        );
    }
}
