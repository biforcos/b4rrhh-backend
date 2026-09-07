package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.command.DeleteContractCommand;
import com.b4rrhh.employee.contract.application.command.PlanContractChangeCommand;
import com.b4rrhh.employee.contract.application.command.UpdateContractCommand;
import com.b4rrhh.employee.contract.application.model.ContractPlan;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractIsACorrectionException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.model.ContractPeriod;
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
 * (backend#52), against the real schema: the contract series is written
 * through the temporal component and the invariants of ADR-057 decide. The
 * catalog is the seeded one: IND/FT1 and TMP/PT1 are real ESP codes with
 * their relation (V29/V52).
 */
@TestSobreEsquemaReal
class ContractTimelineFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY_16 = LocalDate.of(2026, 1, 16);
    private static final LocalDate FEB_1 = LocalDate.of(2026, 2, 1);

    @Autowired
    private CreateContractService createService;
    @Autowired
    private UpdateContractService updateService;
    @Autowired
    private DeleteContractService deleteService;
    @Autowired
    private PlanContractChangeService planService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private String employeeNumber;
    private Long employeeId;

    @BeforeEach
    void anEmployeePresentFromDayOne() {
        employeeNumber = "CT" + (System.nanoTime() % 1_000_000_000L);
        employeeId = DatosDePrueba.empleado(jdbcTemplate, employeeNumber);
        DatosDePrueba.presencia(jdbcTemplate, employeeId, 1, DAY_1, null);
    }

    @Test
    void addingFromTheSixteenthClosesTheOpenOneOnTheFifteenthInsteadOfReturningAConflict() {
        createService.create(create("IND", "FT1", DAY_1, null));

        Contract second = createService.create(create("TMP", "PT1", DAY_16, null));
        entityManager.flush();

        assertEquals(DAY_16, second.getStartDate());
        assertNull(second.getEndDate());
        assertEquals(2, persistedCount());
        assertEquals(DAY_15, persistedEndDate(DAY_1));
        assertNull(persistedEndDate(DAY_16));
        assertEquals("IND", persistedCode(DAY_1));
        assertEquals("TMP", persistedCode(DAY_16));
    }

    @Test
    void addingOneThatLeavesAGapIsRejectedSayingWhichGap() {
        createService.create(create("IND", "FT1", DAY_1, null));

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> createService.create(create("TMP", "PT1", FEB_1, LocalDate.of(2026, 2, 28)))
        );
        entityManager.flush();

        assertEquals(List.of(new ContractPeriod(LocalDate.of(2026, 3, 1), null)), ex.gaps());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(DAY_1));
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        createService.create(create("IND", "FT1", DAY_1, null));
        createService.create(create("TMP", "PT1", DAY_16, null));

        deleteService.delete(new DeleteContractCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_16));
        entityManager.flush();

        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(DAY_1));
    }

    @Test
    void deletingOneInTheMiddleIsRejectedSayingWhichNeighbourToStretch() {
        createService.create(create("IND", "FT1", DAY_1, null));
        createService.create(create("TMP", "PT1", DAY_16, null));
        createService.create(create("IND", "FT1", FEB_1, null));

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> deleteService.delete(new DeleteContractCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_16))
        );
        entityManager.flush();

        assertEquals(List.of(new ContractPeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertEquals(
                List.of(
                        new ContractPeriod(DAY_1, DAY_15),
                        new ContractPeriod(FEB_1, null)
                ),
                ex.stretchCandidates()
        );
        assertEquals(3, persistedCount());
    }

    @Test
    void correctingTheDatesIsJudgedByTheSameInvariants() {
        createService.create(create("IND", "FT1", DAY_1, null));
        createService.create(create("TMP", "PT1", DAY_16, null));

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> updateService.update(new UpdateContractCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_16, FEB_1, null, "TMP", "PT1"
                ))
        );
        entityManager.flush();

        assertEquals(List.of(new ContractPeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertTrue(ex.stretchCandidates().contains(new ContractPeriod(DAY_1, DAY_15)));
        assertEquals(2, persistedCount());
        assertNull(persistedEndDate(DAY_16));
    }

    @Test
    void thePlanCanBeAskedForWithoutApplyingIt() {
        createService.create(create("IND", "FT1", DAY_1, null));

        ContractPlan plan = planService.plan(new PlanContractChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_16, null
        ));
        entityManager.flush();

        assertTrue(plan.isAccepted());
        assertEquals(DAY_1, plan.adjustedOccurrence().before().startDate());
        assertEquals(DAY_15, plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(new ContractPeriod(DAY_1, DAY_15), new ContractPeriod(DAY_16, null)),
                plan.projected()
        );
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(DAY_1));
    }

    @Test
    void addingOnTheStartDateOfTheExistingOneIsRejectedAsItsCorrectionAndPersistsNothing() {
        createService.create(create("IND", "FT1", DAY_1, null));

        ContractIsACorrectionException ex = assertThrows(
                ContractIsACorrectionException.class,
                () -> createService.create(create("TMP", "PT1", DAY_1, null))
        );
        entityManager.flush();

        assertEquals(new ContractPeriod(DAY_1, null), ex.correctedOccurrence());
        assertTrue(ex.getMessage().contains("correct"), ex.getMessage());
        assertEquals(1, persistedCount());
        assertEquals("IND", persistedCode(DAY_1));
    }

    @Test
    void thePlanSaysAnAddOnAnExistingStartDateIsACorrectionOfThatContract() {
        createService.create(create("IND", "FT1", DAY_1, null));

        ContractPlan plan = planService.plan(new PlanContractChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_1, DAY_15
        ));
        entityManager.flush();

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new ContractPeriod(DAY_1, null), plan.correctedOccurrence());
        assertEquals(List.of(new ContractPeriod(DAY_1, DAY_15)), plan.projected());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(DAY_1));
    }

    // The correction the plan proposes, asked for as such, is the PUT.
    @Test
    void theCorrectionAskedForAsSuchReplacesTheCodesWithoutADuplicateRow() {
        createService.create(create("IND", "FT1", DAY_1, null));

        Contract corrected = updateService.update(new UpdateContractCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_1, DAY_1, null, "TMP", "PT1"
        ));
        entityManager.flush();

        assertEquals("TMP", corrected.getContractCode());
        assertEquals(1, persistedCount());
        assertEquals("TMP", persistedCode(DAY_1));
        assertEquals("PT1", persistedSubtypeCode(DAY_1));
    }

    @Test
    void twoContractsCannotStartOnTheSameDayEvenIfEveryOtherGuardFails() {
        insertContractBypassingTheUseCase(DAY_1, DAY_15);

        assertThrows(
                DuplicateKeyException.class,
                () -> insertContractBypassingTheUseCase(DAY_1, null)
        );
    }

    private void insertContractBypassingTheUseCase(LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update(
                """
                insert into employee.contract (
                    employee_id, contract_code, contract_subtype_code, start_date, end_date, created_at, updated_at
                ) values (?, 'IND', 'FT1', ?, ?, current_timestamp, current_timestamp)
                """,
                employeeId,
                startDate,
                endDate
        );
    }

    private CreateContractCommand create(String code, String subtype, LocalDate startDate, LocalDate endDate) {
        return new CreateContractCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, code, subtype, startDate, endDate);
    }

    private int persistedCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from employee.contract where employee_id = ?",
                Integer.class,
                employeeId
        );
    }

    private LocalDate persistedEndDate(LocalDate startDate) {
        return jdbcTemplate.queryForObject(
                "select end_date from employee.contract where employee_id = ? and start_date = ?",
                LocalDate.class,
                employeeId,
                startDate
        );
    }

    private String persistedCode(LocalDate startDate) {
        return jdbcTemplate.queryForObject(
                "select contract_code from employee.contract where employee_id = ? and start_date = ?",
                String.class,
                employeeId,
                startDate
        );
    }

    private String persistedSubtypeCode(LocalDate startDate) {
        return jdbcTemplate.queryForObject(
                "select contract_subtype_code from employee.contract where employee_id = ? and start_date = ?",
                String.class,
                employeeId,
                startDate
        );
    }
}
