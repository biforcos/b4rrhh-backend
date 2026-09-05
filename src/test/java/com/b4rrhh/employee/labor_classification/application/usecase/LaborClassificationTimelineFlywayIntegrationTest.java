package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.DeleteLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.PlanLaborClassificationChangeCommand;
import com.b4rrhh.employee.labor_classification.application.command.UpdateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationIsACorrectionException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
 * (backend#52), against the real schema: the labor classification series is
 * written through the temporal component and the invariants of ADR-057
 * decide. The catalog is the seeded one: the real agreement 99002405011982
 * and its three categories G1, G2 and G3 (V61), all related to it.
 */
@TestSobreEsquemaReal
class LaborClassificationTimelineFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String AGREEMENT = "99002405011982";
    private static final String G1 = "99002405-G1";
    private static final String G2 = "99002405-G2";
    private static final String G3 = "99002405-G3";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY_16 = LocalDate.of(2026, 1, 16);
    private static final LocalDate FEB_1 = LocalDate.of(2026, 2, 1);

    @Autowired
    private CreateLaborClassificationService createService;
    @Autowired
    private UpdateLaborClassificationService updateService;
    @Autowired
    private DeleteLaborClassificationService deleteService;
    @Autowired
    private PlanLaborClassificationChangeService planService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private String employeeNumber;
    private Long employeeId;

    @BeforeEach
    void anEmployeePresentFromDayOne() {
        employeeNumber = "LC" + (System.nanoTime() % 1_000_000_000L);
        employeeId = DatosDePrueba.empleado(jdbcTemplate, employeeNumber);
        DatosDePrueba.presencia(jdbcTemplate, employeeId, 1, DAY_1, null);
    }

    @Test
    void addingFromTheSixteenthClosesTheOpenOneOnTheFifteenthInsteadOfReturningAConflict() {
        createService.create(create(G1, DAY_1, null));

        LaborClassification second = createService.create(create(G2, DAY_16, null));
        entityManager.flush();

        assertEquals(DAY_16, second.getStartDate());
        assertNull(second.getEndDate());
        assertEquals(2, persistedCount());
        assertEquals(DAY_15, persistedEndDate(DAY_1));
        assertNull(persistedEndDate(DAY_16));
        assertEquals(G1, persistedCategory(DAY_1));
        assertEquals(G2, persistedCategory(DAY_16));
    }

    @Test
    void addingOneThatLeavesAGapIsRejectedSayingWhichGap() {
        createService.create(create(G1, DAY_1, null));

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> createService.create(create(G2, FEB_1, LocalDate.of(2026, 2, 28)))
        );
        entityManager.flush();

        assertEquals(List.of(new LaborClassificationPeriod(LocalDate.of(2026, 3, 1), null)), ex.gaps());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(DAY_1));
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        createService.create(create(G1, DAY_1, null));
        createService.create(create(G2, DAY_16, null));

        deleteService.delete(new DeleteLaborClassificationCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_16));
        entityManager.flush();

        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(DAY_1));
    }

    @Test
    void deletingOneInTheMiddleIsRejectedSayingWhichNeighbourToStretch() {
        createService.create(create(G1, DAY_1, null));
        createService.create(create(G2, DAY_16, null));
        createService.create(create(G3, FEB_1, null));

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> deleteService.delete(new DeleteLaborClassificationCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_16))
        );
        entityManager.flush();

        assertEquals(List.of(new LaborClassificationPeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertEquals(
                List.of(
                        new LaborClassificationPeriod(DAY_1, DAY_15),
                        new LaborClassificationPeriod(FEB_1, null)
                ),
                ex.stretchCandidates()
        );
        assertEquals(3, persistedCount());
    }

    @Test
    void correctingTheDatesIsJudgedByTheSameInvariants() {
        createService.create(create(G1, DAY_1, null));
        createService.create(create(G2, DAY_16, null));

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> updateService.update(new UpdateLaborClassificationCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_16, FEB_1, null, AGREEMENT, G2
                ))
        );
        entityManager.flush();

        assertEquals(List.of(new LaborClassificationPeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertTrue(ex.stretchCandidates().contains(new LaborClassificationPeriod(DAY_1, DAY_15)));
        assertEquals(2, persistedCount());
        assertNull(persistedEndDate(DAY_16));
    }

    @Test
    void thePlanCanBeAskedForWithoutApplyingIt() {
        createService.create(create(G1, DAY_1, null));

        LaborClassificationPlan plan = planService.plan(new PlanLaborClassificationChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_16, null
        ));
        entityManager.flush();

        assertTrue(plan.isAccepted());
        assertEquals(DAY_1, plan.adjustedOccurrence().before().startDate());
        assertEquals(DAY_15, plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(new LaborClassificationPeriod(DAY_1, DAY_15), new LaborClassificationPeriod(DAY_16, null)),
                plan.projected()
        );
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(DAY_1));
    }

    @Test
    void addingOnTheStartDateOfTheExistingOneIsRejectedAsItsCorrectionAndPersistsNothing() {
        createService.create(create(G1, DAY_1, null));

        LaborClassificationIsACorrectionException ex = assertThrows(
                LaborClassificationIsACorrectionException.class,
                () -> createService.create(create(G2, DAY_1, null))
        );
        entityManager.flush();

        assertEquals(new LaborClassificationPeriod(DAY_1, null), ex.correctedOccurrence());
        assertTrue(ex.getMessage().contains("correct"), ex.getMessage());
        assertEquals(1, persistedCount());
        assertEquals(G1, persistedCategory(DAY_1));
    }

    @Test
    void thePlanSaysAnAddOnAnExistingStartDateIsACorrectionOfThatOccurrence() {
        createService.create(create(G1, DAY_1, null));

        LaborClassificationPlan plan = planService.plan(new PlanLaborClassificationChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_1, DAY_15
        ));
        entityManager.flush();

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new LaborClassificationPeriod(DAY_1, null), plan.correctedOccurrence());
        assertEquals(List.of(new LaborClassificationPeriod(DAY_1, DAY_15)), plan.projected());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(DAY_1));
    }

    // The correction the plan proposes, asked for as such, is the PUT.
    @Test
    void theCorrectionAskedForAsSuchReplacesTheCategoryWithoutADuplicateRow() {
        createService.create(create(G1, DAY_1, null));

        LaborClassification corrected = updateService.update(new UpdateLaborClassificationCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_1, null, null, AGREEMENT, G2
        ));
        entityManager.flush();

        assertEquals(G2, corrected.getAgreementCategoryCode());
        assertEquals(1, persistedCount());
        assertEquals(G2, persistedCategory(DAY_1));
    }

    @Test
    void twoOccurrencesCannotStartOnTheSameDayEvenIfEveryOtherGuardFails() {
        insertBypassingTheUseCase(DAY_1, DAY_15);

        assertThrows(
                DuplicateKeyException.class,
                () -> insertBypassingTheUseCase(DAY_1, null)
        );
    }

    private void insertBypassingTheUseCase(LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update(
                """
                insert into employee.labor_classification (
                    employee_id, agreement_code, agreement_category_code, start_date, end_date, created_at, updated_at
                ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                employeeId,
                AGREEMENT,
                G1,
                startDate,
                endDate
        );
    }

    private CreateLaborClassificationCommand create(String category, LocalDate startDate, LocalDate endDate) {
        return new CreateLaborClassificationCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, AGREEMENT, category, startDate, endDate);
    }

    private int persistedCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from employee.labor_classification where employee_id = ?",
                Integer.class,
                employeeId
        );
    }

    private LocalDate persistedEndDate(LocalDate startDate) {
        return jdbcTemplate.queryForObject(
                "select end_date from employee.labor_classification where employee_id = ? and start_date = ?",
                LocalDate.class,
                employeeId,
                startDate
        );
    }

    private String persistedCategory(LocalDate startDate) {
        return jdbcTemplate.queryForObject(
                "select agreement_category_code from employee.labor_classification where employee_id = ? and start_date = ?",
                String.class,
                employeeId,
                startDate
        );
    }
}
