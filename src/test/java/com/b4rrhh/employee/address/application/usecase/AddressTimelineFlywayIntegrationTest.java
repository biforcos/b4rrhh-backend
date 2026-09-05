package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.domain.exception.AddressCoverageGapException;
import com.b4rrhh.employee.address.domain.exception.AddressIsACorrectionException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five cases backend#48 is done when they hold, plus the one backend#53
 * adds, against the real schema: the address series is written through the
 * temporal component, one series per employee and type, with the coverage
 * the catalog seeds for ESP (HOME mandatory, FISCAL optional).
 */
@TestSobreEsquemaReal
class AddressTimelineFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String HOME = "HOME";
    private static final String FISCAL = "FISCAL";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY_16 = LocalDate.of(2026, 1, 16);

    @Autowired
    private CreateAddressService createService;
    @Autowired
    private UpdateAddressService updateService;
    @Autowired
    private DeleteAddressService deleteService;
    @Autowired
    private PlanAddressChangeService planService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private String employeeNumber;
    private Long employeeId;

    @BeforeEach
    void anEmployeePresentFromDayOne() {
        employeeNumber = "AD" + (System.nanoTime() % 1_000_000_000L);
        employeeId = DatosDePrueba.empleado(jdbcTemplate, employeeNumber);
        DatosDePrueba.presencia(jdbcTemplate, employeeId, 1, DAY_1, null);
    }

    // Case 1: adding at the end.
    @Test
    void addingFromTheSixteenthClosesTheOpenOneOnTheFifteenthInsteadOfReturningAConflict() {
        createService.create(create(HOME, DAY_1, null));

        Address second = createService.create(create(HOME, DAY_16, null));
        entityManager.flush();

        assertEquals(2, second.getAddressNumber());
        assertEquals(DAY_16, second.getStartDate());
        assertNull(second.getEndDate());
        assertEquals(DAY_15, persistedEndDate(1));
        assertNull(persistedEndDate(2));
    }

    // Case 2: adding in the middle, leaving the domicile uncovered afterwards.
    @Test
    void addingADomicileThatLeavesAGapIsRejectedSayingWhichGap() {
        createService.create(create(HOME, DAY_1, null));

        AddressCoverageGapException ex = assertThrows(
                AddressCoverageGapException.class,
                () -> createService.create(create(HOME, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
        );
        entityManager.flush();

        assertEquals(HOME, ex.addressTypeCode());
        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 3, 1), null)), ex.gaps());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    // Case 3: adding outside the presence. For a working time it is a fault; for an address it is
    // not (backend#53): the person keeps living somewhere after leaving, and the company keeps
    // writing there. Closing the domicile on the termination day, and adding a later one, are
    // both accepted.
    @Test
    void anAddressMayOutliveTheTermination() {
        createService.create(create(HOME, DAY_1, null));
        theEmployeeLeftOn(LocalDate.of(2026, 6, 30));

        Address afterLeaving = createService.create(create(HOME, LocalDate.of(2026, 9, 1), null));
        Address corrected = updateService.update(new UpdateAddressCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2,
                "Calle Nueva 5", "Sevilla", "ESP", null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 12, 31)
        ));
        entityManager.flush();

        assertEquals(2, afterLeaving.getAddressNumber());
        assertEquals(LocalDate.of(2026, 8, 31), persistedEndDate(1));
        assertEquals(LocalDate.of(2027, 12, 31), corrected.getEndDate());
        assertEquals(2, persistedCount());
    }

    // Case 4: deleting the last one.
    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        createService.create(create(HOME, DAY_1, null));
        createService.create(create(HOME, DAY_16, null));

        deleteService.delete(new DeleteAddressCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2));
        entityManager.flush();

        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    // Case 5: deleting one in the middle.
    @Test
    void deletingOneInTheMiddleOfTheDomicileIsRejectedSayingWhichNeighbourToStretch() {
        createService.create(create(HOME, DAY_1, null));
        createService.create(create(HOME, DAY_16, null));
        createService.create(create(HOME, LocalDate.of(2026, 2, 1), null));

        AddressCoverageGapException ex = assertThrows(
                AddressCoverageGapException.class,
                () -> deleteService.delete(new DeleteAddressCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2))
        );
        entityManager.flush();

        assertEquals(List.of(new AddressPeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertEquals(
                List.of(
                        new AddressOccurrence(1, DAY_1, DAY_15),
                        new AddressOccurrence(3, LocalDate.of(2026, 2, 1), null)
                ),
                ex.stretchCandidates()
        );
        assertEquals(3, persistedCount());
    }

    // The one backend#53 adds: HOME and FISCAL at the same time, each edited without the other in the way.
    @Test
    void anEmployeeWithHomeAndFiscalAtTheSameTimeCanEditBothWithoutEitherGettingInTheWay() {
        createService.create(create(HOME, DAY_1, null));
        Address fiscal = createService.create(create(FISCAL, DAY_1, null));
        entityManager.flush();

        // Same start date as the domicile, other series: it is an add, not a correction, and no overlap.
        assertEquals(2, fiscal.getAddressNumber());
        assertNull(persistedEndDate(1));

        // Adding a later fiscal address closes the fiscal one, never the domicile.
        createService.create(create(FISCAL, LocalDate.of(2026, 3, 1), null));
        entityManager.flush();
        assertEquals(LocalDate.of(2026, 2, 28), persistedEndDate(2));
        assertNull(persistedEndDate(1));

        // Deleting the last fiscal one reopens the previous fiscal one; the domicile is untouched.
        deleteService.delete(new DeleteAddressCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 3));
        entityManager.flush();
        assertNull(persistedEndDate(2));

        // A fiscal address may be closed leaving the rest of the presence without one: its coverage is optional.
        updateService.update(new UpdateAddressCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2,
                "Calle Fiscal 1", "Madrid", "ESP", null, null, DAY_1, LocalDate.of(2026, 6, 30)
        ));
        entityManager.flush();
        assertEquals(LocalDate.of(2026, 6, 30), persistedEndDate(2));

        // And the domicile may not: it is the one the employee has to have.
        assertThrows(
                AddressCoverageGapException.class,
                () -> updateService.update(new UpdateAddressCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 1,
                        "Calle Mayor 10", "Madrid", "ESP", null, null, DAY_1, LocalDate.of(2026, 6, 30)
                ))
        );
        entityManager.flush();
        assertNull(persistedEndDate(1));
        assertEquals(2, persistedCount());
    }

    @Test
    void correctingTheDatesIsJudgedByTheSameInvariants() {
        createService.create(create(HOME, DAY_1, null));
        createService.create(create(HOME, DAY_16, null));

        AddressCoverageGapException ex = assertThrows(
                AddressCoverageGapException.class,
                () -> updateService.update(new UpdateAddressCommand(
                        RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, 2,
                        "Calle Mayor 10", "Madrid", "ESP", null, null, LocalDate.of(2026, 2, 1), null
                ))
        );
        entityManager.flush();

        assertEquals(List.of(new AddressPeriod(DAY_16, LocalDate.of(2026, 1, 31))), ex.gaps());
        assertTrue(ex.stretchCandidates().contains(new AddressOccurrence(1, DAY_1, DAY_15)));
        assertEquals(DAY_16, persistedStartDate(2));
    }

    @Test
    void thePlanCanBeAskedForWithoutApplyingIt() {
        createService.create(create(HOME, DAY_1, null));

        AddressPlan plan = planService.plan(new PlanAddressChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, HOME, null, DAY_16, null
        ));
        entityManager.flush();

        assertTrue(plan.isAccepted());
        assertEquals(HOME, plan.addressTypeCode());
        assertEquals(1, plan.adjustedOccurrence().addressNumber());
        assertEquals(DAY_15, plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(
                        new AddressOccurrence(1, DAY_1, DAY_15),
                        new AddressOccurrence(null, DAY_16, null)
                ),
                plan.projected()
        );
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    @Test
    void addingOnTheStartDateOfTheExistingOneOfTheTypeIsRejectedAsItsCorrectionAndPersistsNothing() {
        createService.create(create(HOME, DAY_1, null));

        AddressIsACorrectionException ex = assertThrows(
                AddressIsACorrectionException.class,
                () -> createService.create(create(HOME, DAY_1, DAY_15))
        );
        entityManager.flush();

        assertEquals(new AddressOccurrence(1, DAY_1, null), ex.correctedOccurrence());
        assertEquals(1, persistedCount());
        assertNull(persistedEndDate(1));
    }

    private void theEmployeeLeftOn(LocalDate endDate) {
        jdbcTemplate.update(
                "update employee.presence set end_date = ?, exit_reason_code = 'TERMINATION' where employee_id = ?",
                endDate,
                employeeId
        );
    }

    private CreateAddressCommand create(String addressTypeCode, LocalDate startDate, LocalDate endDate) {
        return new CreateAddressCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber,
                addressTypeCode, "Calle Mayor 10", "Madrid", "ESP", "28013", "MD",
                startDate, endDate
        );
    }

    private int persistedCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from employee.address where employee_id = ?",
                Integer.class,
                employeeId
        );
    }

    private LocalDate persistedEndDate(int addressNumber) {
        return jdbcTemplate.queryForObject(
                "select end_date from employee.address where employee_id = ? and address_number = ?",
                LocalDate.class,
                employeeId,
                addressNumber
        );
    }

    private LocalDate persistedStartDate(int addressNumber) {
        return jdbcTemplate.queryForObject(
                "select start_date from employee.address where employee_id = ? and address_number = ?",
                LocalDate.class,
                employeeId,
                addressNumber
        );
    }
}
