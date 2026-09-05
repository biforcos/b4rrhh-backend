package com.b4rrhh.employee.address.application.service;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.model.AddressPlanAdjustment;
import com.b4rrhh.employee.address.application.port.AddressPresenceLookupPort;
import com.b4rrhh.employee.address.application.port.AddressTypeCoverageLookupPort;
import com.b4rrhh.employee.address.domain.exception.AddressCoverageGapException;
import com.b4rrhh.employee.address.domain.exception.AddressIsACorrectionException;
import com.b4rrhh.employee.address.domain.exception.AddressOverlapException;
import com.b4rrhh.employee.address.domain.exception.AddressTypeCoverageNotDeclaredException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The series under test: the employee is present from 2026-01-01 onwards and
 * has two HOME addresses, the second still open. HOME is the domicile
 * (mandatory); FISCAL is optional. Each type is its own series (ADR-057,
 * decision 0): the timeline the planner sees is the one of the employee and
 * that type, never the employee's addresses as a whole.
 *
 * <pre>
 *   HOME #1  2026-01-01 .. 2026-01-31
 *   HOME #2  2026-02-01 .. (open)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class AddressTimelineServiceTest {

    private static final Long EMPLOYEE_ID = 10L;
    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String HOME = "HOME";
    private static final String FISCAL = "FISCAL";

    private static final Address FIRST = address(1, HOME, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Address SECOND = address(2, HOME, LocalDate.of(2026, 2, 1), null);

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private AddressPresenceLookupPort presencePort;
    @Mock
    private AddressTypeCoverageLookupPort coveragePort;

    private AddressTimelineService service;

    @BeforeEach
    void setUp() {
        service = new AddressTimelineService(addressRepository, presencePort, coveragePort);
    }

    @Test
    void addingAfterTheOpenOneClosesItTheDayBeforeAndNamesItByNumber() {
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, SECOND);

        AddressPlan plan = service.planAdd(EMPLOYEE_ID, RULE_SYSTEM_CODE, HOME, range(LocalDate.of(2026, 3, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(HOME, plan.addressTypeCode());
        assertEquals(new AddressOccurrence(null, LocalDate.of(2026, 3, 16), null), plan.occurrence());
        assertEquals(
                new AddressPlanAdjustment(
                        2,
                        new AddressPeriod(LocalDate.of(2026, 2, 1), null),
                        new AddressPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15))
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(
                        new AddressOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new AddressOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15)),
                        new AddressOccurrence(null, LocalDate.of(2026, 3, 16), null)
                ),
                plan.projected()
        );
    }

    @Test
    void theSeriesIsTheOneOfTheTypeSoAFiscalAddressNeverSeesTheHomeOnes() {
        givenSeries(FISCAL, TimelineCoverage.OPTIONAL);

        AddressPlan plan = service.planAdd(EMPLOYEE_ID, RULE_SYSTEM_CODE, FISCAL, range(LocalDate.of(2026, 2, 1), null));

        assertTrue(plan.isAccepted());
        assertNull(plan.adjustedOccurrence());
        assertEquals(List.of(new AddressOccurrence(null, LocalDate.of(2026, 2, 1), null)), plan.projected());
        verify(addressRepository, never()).findByEmployeeIdAndAddressTypeCodeOrderByStartDate(EMPLOYEE_ID, HOME);
        verify(addressRepository, never()).findByEmployeeIdOrderByStartDate(EMPLOYEE_ID);
    }

    @Test
    void anOptionalTypeMayLeaveThePresenceUncovered() {
        givenSeries(FISCAL, TimelineCoverage.OPTIONAL);

        AddressPlan plan = service.planAdd(EMPLOYEE_ID, RULE_SYSTEM_CODE, FISCAL, range(LocalDate.of(2026, 6, 1), null));

        assertTrue(plan.isAccepted());
        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31))), plan.gaps());
    }

    @Test
    void theDomicileMayNotLeaveThePresenceUncoveredAndTheGapNamesTheNeighboursByNumber() {
        Address closedSecond = address(2, HOME, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, closedSecond);

        AddressPlan plan = service.planAdd(EMPLOYEE_ID, RULE_SYSTEM_CODE, HOME, range(LocalDate.of(2026, 4, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))), plan.gaps());
        assertEquals(
                List.of(
                        new AddressOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
                        new AddressOccurrence(null, LocalDate.of(2026, 4, 1), null)
                ),
                plan.stretchCandidates()
        );
    }

    @Test
    void anAddressMayStartBeforeTheHireAndOutliveTheTermination() {
        when(addressRepository.findByEmployeeIdAndAddressTypeCodeOrderByStartDate(EMPLOYEE_ID, HOME))
                .thenReturn(List.of(FIRST, SECOND));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, HOME)).thenReturn(Optional.of(TimelineCoverage.MANDATORY));

        AddressPlan beforeTheHire = service.planAdd(
                EMPLOYEE_ID, RULE_SYSTEM_CODE, HOME, range(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 12, 31))
        );
        AddressPlan afterTheTermination = service.planCorrect(
                EMPLOYEE_ID, RULE_SYSTEM_CODE, SECOND, range(LocalDate.of(2026, 2, 1), LocalDate.of(2027, 12, 31))
        );

        assertTrue(beforeTheHire.isAccepted());
        assertTrue(afterTheTermination.isAccepted());
    }

    @Test
    void removingTheLastOneReopensThePreviousOne() {
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, SECOND);

        AddressPlan plan = service.planRemove(EMPLOYEE_ID, RULE_SYSTEM_CODE, SECOND);

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(new AddressOccurrence(2, LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(
                new AddressPlanAdjustment(
                        1,
                        new AddressPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new AddressPeriod(LocalDate.of(2026, 1, 1), null)
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(List.of(new AddressOccurrence(1, LocalDate.of(2026, 1, 1), null)), plan.projected());
    }

    @Test
    void removingOneInTheMiddleOfTheDomicileIsRejectedNamingTheNeighboursToStretch() {
        Address closedSecond = address(2, HOME, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        Address third = address(3, HOME, LocalDate.of(2026, 3, 1), null);
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, closedSecond, third);

        AddressPlan plan = service.planRemove(EMPLOYEE_ID, RULE_SYSTEM_CODE, closedSecond);

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), plan.gaps());
        assertEquals(
                List.of(
                        new AddressOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new AddressOccurrence(3, LocalDate.of(2026, 3, 1), null)
                ),
                plan.stretchCandidates()
        );
    }

    @Test
    void correctingKeepsTheNumberUnderTheNewDates() {
        Address closedTooEarly = address(2, HOME, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, closedTooEarly);

        AddressPlan plan = service.planCorrect(EMPLOYEE_ID, RULE_SYSTEM_CODE, closedTooEarly, range(LocalDate.of(2026, 2, 1), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertNull(plan.adjustedOccurrence());
        assertEquals(new AddressOccurrence(2, LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(new AddressOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)), plan.correctedOccurrence());
    }

    @Test
    void addingOnTheStartDateOfAnExistingOneIsRejectedAsItsCorrectionAndNamesItByNumber() {
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, SECOND);

        AddressPlan plan = service.planAdd(
                EMPLOYEE_ID, RULE_SYSTEM_CODE, HOME, range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31))
        );

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new AddressOccurrence(2, LocalDate.of(2026, 2, 1), null), plan.correctedOccurrence());
        assertEquals(new AddressOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)), plan.occurrence());
    }

    @Test
    void aTypeWhoseCoverageTheCatalogDoesNotDeclareCannotBePlanned() {
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, "TEMPORARY")).thenReturn(Optional.empty());

        assertThrows(
                AddressTypeCoverageNotDeclaredException.class,
                () -> service.planAdd(EMPLOYEE_ID, RULE_SYSTEM_CODE, "TEMPORARY", range(LocalDate.of(2026, 2, 1), null))
        );
        verify(addressRepository, never()).findByEmployeeIdAndAddressTypeCodeOrderByStartDate(eq(EMPLOYEE_ID), anyString());
    }

    @Test
    void aPlanRejectedAsACorrectionBecomesTheIsACorrectionExceptionNamingTheAddressToCorrect() {
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, SECOND);
        AddressPlan plan = service.planAdd(EMPLOYEE_ID, RULE_SYSTEM_CODE, HOME, range(LocalDate.of(2026, 2, 1), null));

        AddressIsACorrectionException ex = assertThrows(
                AddressIsACorrectionException.class,
                () -> service.requireAccepted(plan, RULE_SYSTEM_CODE, "INTERNAL", "EMP001")
        );

        assertEquals(new AddressOccurrence(2, LocalDate.of(2026, 2, 1), null), ex.correctedOccurrence());
        assertEquals(new AddressPeriod(LocalDate.of(2026, 2, 1), null), ex.requested());
    }

    @Test
    void aRejectedPlanForAGapBecomesTheGapExceptionNamingTheType() {
        Address closedSecond = address(2, HOME, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, closedSecond);
        AddressPlan plan = service.planAdd(EMPLOYEE_ID, RULE_SYSTEM_CODE, HOME, range(LocalDate.of(2026, 4, 1), null));

        AddressCoverageGapException ex = assertThrows(
                AddressCoverageGapException.class,
                () -> service.requireAccepted(plan, RULE_SYSTEM_CODE, "INTERNAL", "EMP001")
        );

        assertEquals(HOME, ex.addressTypeCode());
        assertEquals(plan.gaps(), ex.gaps());
        assertEquals(plan.stretchCandidates(), ex.stretchCandidates());
    }

    @Test
    void aRejectedPlanForAnOverlapBecomesTheOverlapExceptionWithTheSharedDates() {
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, SECOND);
        AddressPlan plan = service.planAdd(
                EMPLOYEE_ID, RULE_SYSTEM_CODE, HOME, range(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10))
        );

        AddressOverlapException ex = assertThrows(
                AddressOverlapException.class,
                () -> service.requireAccepted(plan, RULE_SYSTEM_CODE, "INTERNAL", "EMP001")
        );

        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
    }

    @Test
    void anAcceptedPlanPassesThrough() {
        givenSeries(HOME, TimelineCoverage.MANDATORY, FIRST, SECOND);
        AddressPlan plan = service.planAdd(EMPLOYEE_ID, RULE_SYSTEM_CODE, HOME, range(LocalDate.of(2026, 3, 16), null));

        service.requireAccepted(plan, RULE_SYSTEM_CODE, "INTERNAL", "EMP001");
    }

    private void givenSeries(String addressTypeCode, TimelineCoverage coverage, Address... occurrences) {
        when(addressRepository.findByEmployeeIdAndAddressTypeCodeOrderByStartDate(EMPLOYEE_ID, addressTypeCode))
                .thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(range(LocalDate.of(2026, 1, 1), null)));
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, addressTypeCode)).thenReturn(Optional.of(coverage));
    }

    private static DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    private static Address address(int number, String addressTypeCode, LocalDate startDate, LocalDate endDate) {
        return new Address(
                (long) number,
                EMPLOYEE_ID,
                number,
                addressTypeCode,
                "Calle Mayor " + number,
                "Madrid",
                "ESP",
                "28013",
                "MD",
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
